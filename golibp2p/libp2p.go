// Package golibp2p is a narrow, gomobile-bindable wrapper over go-libp2p
// (ADR-0004). It is PURE TRANSPORT: it shuttles opaque []byte and never parses
// records. All DAG-CBOR/signing/verification/relay logic stays in Kotlin.
//
// Only []byte/string/int/bool/error and the callback interfaces below cross the
// gomobile boundary; go-libp2p types stay unexported.
package golibp2p

import (
	"context"
	"crypto/ed25519"
	"encoding/hex"
	"fmt"
	"strings"
	"sync"

	"github.com/ipfs/go-cid"
	dht "github.com/libp2p/go-libp2p-kad-dht"
	pubsub "github.com/libp2p/go-libp2p-pubsub"
	record "github.com/libp2p/go-libp2p-record"
	"github.com/libp2p/go-libp2p"
	"github.com/libp2p/go-libp2p/core/crypto"
	"github.com/libp2p/go-libp2p/core/host"
	"github.com/libp2p/go-libp2p/core/network"
	"github.com/libp2p/go-libp2p/core/peer"
	"github.com/libp2p/go-libp2p/core/protocol"
	"github.com/libp2p/go-libp2p/p2p/discovery/mdns"
	mh "github.com/multiformats/go-multihash"
)

// ---- callback interfaces (Kotlin implements these; bridged to Flow) ----

type GossipCallback interface {
	OnMessage(topic string, from string, payload []byte)
}

type ProviderCallback interface {
	OnProvider(peerID string)
}

type StreamCallback interface {
	OnStream(s *Stream)
}

// ---- Host ----

type Host struct {
	ctx    context.Context
	cancel context.CancelFunc
	h      host.Host
	kdht   *dht.IpfsDHT
	ps     *pubsub.PubSub
	prefix string

	enableMdns bool

	reachMu sync.Mutex
	reach   network.Reachability // AutoNAT verdict, updated from the event bus (Idea C)

	mu     sync.Mutex
	topics map[string]*pubsub.Topic
	subs   map[string]*pubsub.Subscription
}

// NewHost builds a go-libp2p host whose identity is the raw 32-byte Ed25519
// transport seed (ADR-0003). listenAddrsCSV/bootstrapCSV are comma-separated
// multiaddr strings. protocolPrefix namespaces protocols/DHT (e.g. "/aura").
// enableMdns toggles go-libp2p's built-in mDNS: keep it OFF on Android (SELinux
// blocks netlink interface enumeration, b/155595000; LAN discovery is done via
// Android NsdManager instead). Desktop/tests pass true.
func NewHost(seed []byte, listenAddrsCSV string, bootstrapCSV string, protocolPrefix string, enableMdns bool) (*Host, error) {
	priv, err := privFromSeed(seed)
	if err != nil {
		return nil, err
	}
	prefix := protocolPrefix
	if prefix == "" {
		prefix = "/aura"
	}
	ctx, cancel := context.WithCancel(context.Background())

	listen := splitNonEmpty(listenAddrsCSV)
	if len(listen) == 0 {
		listen = []string{
			"/ip4/0.0.0.0/tcp/0",
			"/ip4/0.0.0.0/udp/0/quic-v1",
			"/ip6/::/tcp/0",
			"/ip6/::/udp/0/quic-v1",
		}
	}

	opts := []libp2p.Option{
		libp2p.Identity(priv),
		libp2p.ListenAddrStrings(listen...),
		libp2p.DefaultTransports,
		libp2p.DefaultSecurity,
		libp2p.DefaultMuxers,
		libp2p.EnableNATService(),
		libp2p.EnableHolePunching(),
		libp2p.EnableRelay(), // relay transport: required so DCUtR can coordinate a hole punch
		// Open AutoRelay (EnableAutoRelayWithPeerSource over the DHT) stays OFF: it routes app
		// traffic through arbitrary third-party relays, exposing connection metadata even though
		// payloads stay E2EE. The phase-2 efficiency path instead allows a TRUSTED static relay set
		// (the app's own reachable FGS nodes) via SetAutoRelayStaticRelays + a limited relay SERVICE
		// via SetRelayServiceEnabled (both default-off) - see efficiency.go / INTEGRATION.md. This
		// recovers DCUtR coordination + a tiny real-time path for the CGNAT majority without an open
		// relay source. IPFS remains only a transient cold-entry rendezvous (ADR-0002).
	}
	opts = append(opts, efficiencyOptions()...)
	h, err := libp2p.New(opts...)
	if err != nil {
		cancel()
		return nil, err
	}

	host := &Host{
		ctx:        ctx,
		cancel:     cancel,
		h:          h,
		prefix:     prefix,
		enableMdns: enableMdns,
		topics:     map[string]*pubsub.Topic{},
		subs:       map[string]*pubsub.Subscription{},
	}
	return host, nil
}

// Start brings up the DHT (server mode), GossipSub, and mDNS discovery.
func (host *Host) Start() error {
	ns := strings.TrimPrefix(host.prefix, "/")

	// Custom protocol prefix => app-private DHT (/aura/kad/1.0.0) and avoids
	// the default /ipfs prefix's mandatory /pk + /ipns validators (ADR-0002).
	kdht, err := dht.New(
		host.ctx, host.h,
		dht.Mode(dht.ModeServer),
		dht.ProtocolPrefix(protocol.ID(host.prefix)),
		dht.NamespacedValidator(ns, blankValidator{}),
	)
	if err != nil {
		return err
	}
	if err := kdht.Bootstrap(host.ctx); err != nil {
		return err
	}
	host.kdht = kdht

	ps, err := pubsub.NewGossipSub(host.ctx, host.h, gossipOptions()...)
	if err != nil {
		return err
	}
	host.ps = ps

	// Idea C: start caching AutoNAT's reachability verdict for the Kotlin reachability gate.
	host.startReachabilityWatch()

	// go-libp2p mDNS: only on platforms where interface enumeration works
	// (desktop). Off on Android — see [NewHost]. Best-effort either way.
	if host.enableMdns {
		svc := mdns.NewMdnsService(host.h, ns, &mdnsNotifee{ctx: host.ctx, h: host.h})
		_ = svc.Start()
	}
	return nil
}

func (host *Host) Stop() error {
	host.cancel()
	if host.kdht != nil {
		_ = host.kdht.Close()
	}
	return host.h.Close()
}

func (host *Host) PeerID() string { return host.h.ID().String() }

// Connect dials a peer by full multiaddr ("/ip4/.../tcp/.../p2p/<peerid>").
// Used for explicit dial (bootstrap, relay, tests) when discovery has not
// already populated the peerstore.
func (host *Host) Connect(maddr string) error {
	ai, err := peer.AddrInfoFromString(maddr)
	if err != nil {
		return err
	}
	return host.h.Connect(host.ctx, *ai)
}

// ListenAddrs returns the host's multiaddrs, newline-separated.
func (host *Host) ListenAddrs() string {
	var b strings.Builder
	for i, a := range host.h.Addrs() {
		if i > 0 {
			b.WriteByte('\n')
		}
		b.WriteString(a.String())
	}
	return b.String()
}

// ---- DHT (records keyed under the namespace; opaque values) ----

func (host *Host) DHTPut(key []byte, value []byte) error {
	return host.kdht.PutValue(host.ctx, host.recordKey(key), value)
}

func (host *Host) DHTGet(key []byte, max int) (*ByteList, error) {
	v, err := host.kdht.GetValue(host.ctx, host.recordKey(key))
	if err != nil {
		return nil, err
	}
	return &ByteList{items: [][]byte{v}}, nil
}

func (host *Host) DHTProvide(key []byte) error {
	c, err := keyToCID(key)
	if err != nil {
		return err
	}
	return host.kdht.Provide(host.ctx, c, true)
}

func (host *Host) DHTFindProviders(key []byte, cb ProviderCallback) {
	go func() {
		c, err := keyToCID(key)
		if err != nil {
			return
		}
		for p := range host.kdht.FindProvidersAsync(host.ctx, c, 0) {
			cb.OnProvider(p.ID.String())
		}
	}()
}

// ---- GossipSub ----

func (host *Host) GossipSubscribe(topic string, cb GossipCallback) error {
	t, err := host.joinTopic(topic)
	if err != nil {
		return err
	}
	sub, err := t.Subscribe()
	if err != nil {
		return err
	}
	host.mu.Lock()
	host.subs[topic] = sub
	host.mu.Unlock()

	go func() {
		self := host.h.ID()
		for {
			msg, err := sub.Next(host.ctx)
			if err != nil {
				return // ctx cancelled or sub closed
			}
			if msg.ReceivedFrom == self {
				continue // skip our own messages
			}
			cb.OnMessage(topic, msg.GetFrom().String(), msg.GetData())
		}
	}()
	return nil
}

func (host *Host) GossipUnsubscribe(topic string) error {
	host.mu.Lock()
	defer host.mu.Unlock()
	if sub, ok := host.subs[topic]; ok {
		sub.Cancel()
		delete(host.subs, topic)
	}
	return nil
}

func (host *Host) GossipPublish(topic string, payload []byte) error {
	t, err := host.joinTopic(topic)
	if err != nil {
		return err
	}
	return t.Publish(host.ctx, payload)
}

// ---- Streams ----

func (host *Host) RegisterStreamHandler(protocolID string, cb StreamCallback) {
	host.h.SetStreamHandler(protocol.ID(protocolID), func(s network.Stream) {
		cb.OnStream(&Stream{s: s})
	})
}

func (host *Host) OpenStream(remotePeerID string, protocolID string) (*Stream, error) {
	pid, err := peer.Decode(remotePeerID)
	if err != nil {
		return nil, err
	}
	s, err := host.h.NewStream(host.ctx, pid, protocol.ID(protocolID))
	if err != nil {
		return nil, err
	}
	return &Stream{s: s}, nil
}

type Stream struct {
	s network.Stream
}

// Read reads up to max bytes (one frame); returns the bytes actually read.
func (st *Stream) Read(max int) ([]byte, error) {
	buf := make([]byte, max)
	n, err := st.s.Read(buf)
	if n > 0 {
		return buf[:n], nil
	}
	return nil, err
}

func (st *Stream) Write(b []byte) error {
	_, err := st.s.Write(b)
	return err
}

func (st *Stream) Close() error { return st.s.Close() }

// ---- ByteList (gomobile cannot return [][]byte) ----

type ByteList struct {
	items [][]byte
}

func (b *ByteList) Len() int          { return len(b.items) }
func (b *ByteList) Get(i int) []byte  { return b.items[i] }

// ---- internals (unexported; not part of the gomobile surface) ----

func (host *Host) joinTopic(topic string) (*pubsub.Topic, error) {
	host.mu.Lock()
	defer host.mu.Unlock()
	if t, ok := host.topics[topic]; ok {
		return t, nil
	}
	t, err := host.ps.Join(topic)
	if err != nil {
		return nil, err
	}
	host.topics[topic] = t
	return t, nil
}

func (host *Host) recordKey(key []byte) string {
	return "/" + strings.TrimPrefix(host.prefix, "/") + "/" + hex.EncodeToString(key)
}

func privFromSeed(seed []byte) (crypto.PrivKey, error) {
	if len(seed) != ed25519.SeedSize {
		return nil, fmt.Errorf("seed must be %d bytes, got %d", ed25519.SeedSize, len(seed))
	}
	return crypto.UnmarshalEd25519PrivateKey(ed25519.NewKeyFromSeed(seed))
}

func splitNonEmpty(csv string) []string {
	var out []string
	for _, s := range strings.Split(csv, ",") {
		s = strings.TrimSpace(s)
		if s != "" {
			out = append(out, s)
		}
	}
	return out
}

func keyToCID(key []byte) (cid.Cid, error) {
	h, err := mh.Sum(key, mh.SHA2_256, -1)
	if err != nil {
		return cid.Undef, err
	}
	return cid.NewCidV1(cid.Raw, h), nil
}

// blankValidator accepts any record; validation is Kotlin's job (ADR-0004).
type blankValidator struct{}

func (blankValidator) Validate(_ string, _ []byte) error        { return nil }
func (blankValidator) Select(_ string, _ [][]byte) (int, error) { return 0, nil }

var _ record.Validator = blankValidator{}

// mdnsNotifee dials peers discovered on the LAN.
type mdnsNotifee struct {
	ctx context.Context
	h   host.Host
}

func (n *mdnsNotifee) HandlePeerFound(pi peer.AddrInfo) {
	_ = n.h.Connect(n.ctx, pi)
}
