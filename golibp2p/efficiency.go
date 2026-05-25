// Phase-2 efficiency / reachability capabilities for the Aura host (sim-results phase2).
//
// These are ADDITIVE and DEFAULT-OFF so the existing verified behaviour is unchanged until the app
// opts in (set the toggles BEFORE creating the host). They give the Kotlin side what it needs for:
//   - Idea C reachability gating: AutoNAT reachability via [Host.Reachability].
//   - the relay/holder tier: a limited circuit-relay-v2 SERVICE (EnableRelayService) so reachable
//     opt-in nodes can coordinate DCUtR hole punches + carry tiny real-time messages for the CGNAT
//     majority (the trusted-relay-AutoRelay direction - the static relay set is the app's own
//     reachable FGS nodes, NOT arbitrary public relays, preserving the no-open-AutoRelay stance).
//   - GossipSub hardening: peer scoring (libp2p pubsub best-practice; StrictSign stays on).
//
// gomobile note: only the exported Set*/Reachability functions cross the boundary; toggles are
// unexported vars. After changing these you MUST rebuild the .aar (gomobile bind). See INTEGRATION.md.
package golibp2p

import (
	"time"

	"github.com/libp2p/go-libp2p"
	"github.com/libp2p/go-libp2p/core/event"
	"github.com/libp2p/go-libp2p/core/network"
	"github.com/libp2p/go-libp2p/core/peer"
	pubsub "github.com/libp2p/go-libp2p-pubsub"
)

var (
	relayServiceEnabled   = false
	gossipHardeningEnabled = false
	autoRelayStaticRelays = "" // comma-separated multiaddrs (trusted/self reachable nodes)
)

// SetRelayServiceEnabled turns on a limited circuit-relay-v2 service on this host (relay-only /
// reachable tier). Call BEFORE NewHost. Only meaningful on a publicly reachable node.
func SetRelayServiceEnabled(b bool) { relayServiceEnabled = b }

// SetAutoRelayStaticRelays enables AutoRelay bound to a fixed, comma-separated set of TRUSTED relay
// multiaddrs (the app's own reachable nodes), not the open DHT. Empty = AutoRelay off. Call BEFORE NewHost.
func SetAutoRelayStaticRelays(csv string) { autoRelayStaticRelays = csv }

// SetGossipHardeningEnabled turns on GossipSub peer scoring. Call BEFORE Start. Validate the score
// params against the app's topics when first enabled.
func SetGossipHardeningEnabled(b bool) { gossipHardeningEnabled = b }

// efficiencyOptions are the extra libp2p.New options implied by the toggles above (all default-off).
func efficiencyOptions() []libp2p.Option {
	var opts []libp2p.Option
	if relayServiceEnabled {
		opts = append(opts, libp2p.EnableRelayService())
	}
	if relays := splitNonEmpty(autoRelayStaticRelays); len(relays) > 0 {
		infos := make([]peer.AddrInfo, 0, len(relays))
		for _, m := range relays {
			if ai, err := peer.AddrInfoFromString(m); err == nil {
				infos = append(infos, *ai)
			}
		}
		if len(infos) > 0 {
			opts = append(opts, libp2p.EnableAutoRelayWithStaticRelays(infos))
		}
	}
	return opts
}

// gossipOptions are the extra GossipSub options (peer scoring) when hardening is enabled.
func gossipOptions() []pubsub.Option {
	if !gossipHardeningEnabled {
		return nil
	}
	// Canonical conservative scoring (eth2-derived defaults): penalise IP colocation + misbehaviour
	// without per-topic params. StrictSign remains the pubsub default. Tune per-topic on rollout.
	params := &pubsub.PeerScoreParams{
		Topics:                      make(map[string]*pubsub.TopicScoreParams),
		TopicScoreCap:               32.72,
		AppSpecificScore:            func(p peer.ID) float64 { return 0 },
		AppSpecificWeight:           1,
		IPColocationFactorWeight:    -35.11,
		IPColocationFactorThreshold: 10,
		BehaviourPenaltyWeight:      -15.92,
		BehaviourPenaltyThreshold:   6,
		BehaviourPenaltyDecay:       0.928,
		DecayInterval:               12 * time.Second,
		DecayToZero:                 0.01,
		RetainScore:                 12 * time.Hour,
	}
	thresholds := &pubsub.PeerScoreThresholds{
		GossipThreshold:             -4000,
		PublishThreshold:            -8000,
		GraylistThreshold:           -16000,
		AcceptPXThreshold:           100,
		OpportunisticGraftThreshold: 5,
	}
	return []pubsub.Option{pubsub.WithPeerScore(params, thresholds)}
}

// startReachabilityWatch caches AutoNAT's verdict from the event bus so [Reachability] can return it
// (Idea C). Best-effort: if the subscription fails, Reachability stays "Unknown" (fail-open gate).
func (host *Host) startReachabilityWatch() {
	sub, err := host.h.EventBus().Subscribe(new(event.EvtLocalReachabilityChanged))
	if err != nil {
		return
	}
	go func() {
		defer sub.Close()
		for {
			select {
			case <-host.ctx.Done():
				return
			case e, ok := <-sub.Out():
				if !ok {
					return
				}
				if ev, ok := e.(event.EvtLocalReachabilityChanged); ok {
					host.reachMu.Lock()
					host.reach = ev.Reachability
					host.reachMu.Unlock()
				}
			}
		}
	}()
}

// Reachability returns AutoNAT's current verdict: "Public", "Private", or "Unknown" (Idea C signal
// for the Kotlin reachability gate). Bindable across gomobile (string return).
func (host *Host) Reachability() string {
	host.reachMu.Lock()
	r := host.reach
	host.reachMu.Unlock()
	switch r {
	case network.ReachabilityPublic:
		return "Public"
	case network.ReachabilityPrivate:
		return "Private"
	default:
		return "Unknown"
	}
}
