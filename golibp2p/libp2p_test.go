package golibp2p

import (
	"testing"
	"time"
)

// Host-level mDNS discovery test (no emulator). Two go-libp2p hosts on the host
// machine should mDNS-discover each other and auto-connect (mdnsNotifee dials on
// HandlePeerFound). Validates the wrapper's mDNS path natively — the desktop OS
// carries multicast, so no Android MulticastLock is needed here.
func TestMDNSDiscoveryAutoConnects(t *testing.T) {
	seedA := make([]byte, 32)
	seedA[0] = 1
	seedB := make([]byte, 32)
	seedB[0] = 2

	a, err := NewHost(seedA, "/ip4/0.0.0.0/tcp/0", "", "/datingapp", true)
	if err != nil {
		t.Fatalf("NewHost A: %v", err)
	}
	defer a.Stop()
	b, err := NewHost(seedB, "/ip4/0.0.0.0/tcp/0", "", "/datingapp", true)
	if err != nil {
		t.Fatalf("NewHost B: %v", err)
	}
	defer b.Stop()

	if err := a.Start(); err != nil {
		t.Fatalf("Start A: %v", err)
	}
	if err := b.Start(); err != nil {
		t.Fatalf("Start B: %v", err)
	}

	deadline := time.Now().Add(30 * time.Second)
	for time.Now().Before(deadline) {
		if len(a.h.Network().Peers()) > 0 && len(b.h.Network().Peers()) > 0 {
			t.Logf("mDNS auto-connected: A peers=%d B peers=%d", len(a.h.Network().Peers()), len(b.h.Network().Peers()))
			return
		}
		time.Sleep(500 * time.Millisecond)
	}
	t.Fatalf("mDNS discovery timed out: A peers=%d B peers=%d",
		len(a.h.Network().Peers()), len(b.h.Network().Peers()))
}

// The libp2p PeerId must be deterministic from the 32-byte Ed25519 transport
// seed (B4/B5 risk item: the app stores the seed, the go host derives the PeerId).
func TestPeerIDDeterministicFromSeed(t *testing.T) {
	seed := make([]byte, 32)
	for i := range seed {
		seed[i] = byte(i)
	}
	a, err := NewHost(seed, "/ip4/127.0.0.1/tcp/0", "", "/datingapp", false)
	if err != nil {
		t.Fatalf("NewHost A: %v", err)
	}
	defer a.Stop()
	b, err := NewHost(seed, "/ip4/127.0.0.1/tcp/0", "", "/datingapp", false)
	if err != nil {
		t.Fatalf("NewHost B: %v", err)
	}
	defer b.Stop()

	if a.PeerID() != b.PeerID() {
		t.Fatalf("PeerID not deterministic from seed: %s vs %s", a.PeerID(), b.PeerID())
	}
	t.Logf("deterministic PeerID=%s", a.PeerID())
}
