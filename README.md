# Aura: A Peer-to-Peer Architecture for Decentralised Social Applications

**Final Project Report**

TU856 · BSc in Computer Science\
School of Computer Science, Technological University Dublin

**Author:** Fuad Godzhaev (D20124630)\
**Supervisor:** Bianca Schoen Phelan\
**Date:** 25.05.2026

## Abstract

This report presents Aura, a peer-to-peer architecture for mobile social applications that runs with no operator-run server, in which every device acts as both its owner's data store and a relay for the network. The recurring obstacle for decentralised social software is not interaction among known contacts, which messengers such as Briar and append-only systems such as Scuttlebutt already handle, nor publishing, which Bluesky, Mastodon, and Nostr decentralise; it is the discovery of strangers with no central index to broker it. This project builds a general substrate for that problem, comprising self-sovereign identity, signed content-addressed records, geohash-keyed discovery over a Kademlia DHT and GossipSub, and end-to-end encrypted messaging with peer store-and-forward, and stress-tests it with a dating application, the social-application class in which stranger discovery is mandatory rather than optional and the privacy stakes are highest [2, 3, 6]. A comparative analysis shows that dating's base-level capability kit is heavier than other social applications' on discovery, location, mutual-consent matching, and safety, but markedly lighter on the broadcast fan-out, global feed ranking, and unbounded content growth that dominate decentralised microblogging and gossip systems [14, 17]. The system was tested via calibrated simulations. The main finding is that delivery with no central server is viable, at approximately 84% of messages on the conservative assumption and approximately 93% on the measurement-backed assumption, with a background battery cost of approximately 9% per day, and that, contrary to common assumption, the distributed hash table is not the bottleneck: its record findability, measured at scale, is complete. The binding constraint is instead the physical supply of inbound-reachable serving nodes, a floor that any decentralised social application requiring stranger discovery would inherit. The sole external dependency is the public IPFS DHT, that could be used transiently and optionally as a last-resort cold-start rendezvous; the system therefore claims no operator-run server or a list of servers, rather than the absence of all infrastructure. The contribution is a working architecture for decentralised stranger-discovery social applications and a measurement-backed map of where the no-central-server ideal meets the limits of the consumer mobile network.

## Declaration

I hereby declare that the work described in this dissertation is, except where otherwise stated, entirely my own work and has not been submitted as an exercise for a degree at this or any other university.

Signed: Fuad Godzhaev, 25.05.2026

## Acknowledgements

I would like to thank my family for supporting me throughout these years, project supervisor for their guidance, and the open-source communities behind go-libp2p, the AT Protocol, Briar, and Secure Scuttlebutt, whose work in decentralised systems informed this project.

## Contents

- [List of Figures and Tables](#list-of-figures-and-tables)
- [1. Introduction](#1-introduction)
  - [1.1 Project Background](#11-project-background)
  - [1.2 Project Description](#12-project-description)
  - [1.3 Project Aims and Objectives](#13-project-aims-and-objectives)
  - [1.4 Project Scope](#14-project-scope)
  - [1.5 Thesis Roadmap](#15-thesis-roadmap)
- [2. Literature Review](#2-literature-review)
  - [2.1 Introduction](#21-introduction)
  - [2.2 Alternative Existing Solutions](#22-alternative-existing-solutions)
  - [2.3 Technologies Researched](#23-technologies-researched)
  - [2.4 Other Relevant Research](#24-other-relevant-research)
  - [2.5 Existing Final Year Projects](#25-existing-final-year-projects)
  - [2.6 Conclusions](#26-conclusions)
- [3. System Analysis](#3-system-analysis)
  - [3.1 System Overview](#31-system-overview)
  - [3.2 The Base-Level Kit: Dating Among Decentralised Social Applications](#32-the-base-level-kit-dating-among-decentralised-social-applications)
  - [3.3 Requirements Gathering](#33-requirements-gathering)
  - [3.4 Requirements Analysis](#34-requirements-analysis)
  - [3.5 Logical Architecture of System](#35-logical-architecture-of-system)
  - [3.6 Initial System Specification](#36-initial-system-specification)
  - [3.7 Conclusions](#37-conclusions)
- [4. System / Experiment Design](#4-system--experiment-design)
  - [4.1 Introduction](#41-introduction)
  - [4.2 Software Methodology](#42-software-methodology)
  - [4.3 Project Planning](#43-project-planning)
    - [4.3.1 Project Schedule](#431-project-schedule)
  - [4.4 Overview of System](#44-overview-of-system)
    - [4.4.1 Fulfilment of System Requirements](#441-fulfilment-of-system-requirements)
  - [4.5 Design Decisions: the Four ADRs](#45-design-decisions-the-four-adrs)
  - [4.6 Detailed Design Topics](#46-detailed-design-topics)
    - [4.6.1 Messaging and End-to-End Encryption](#461-messaging-and-end-to-end-encryption)
    - [4.6.2 Discovery, the Fetch Cascade, and Abuse Posture](#462-discovery-the-fetch-cascade-and-abuse-posture)
    - [4.6.3 The Relay Cache and Mailbox](#463-the-relay-cache-and-mailbox)
    - [4.6.4 Background Operation, Bootstrap, and Connectivity Posture](#464-background-operation-bootstrap-and-connectivity-posture)
- [5. System / Experiment Development](#5-system--experiment-development)
  - [5.1 Introduction](#51-introduction)
  - [5.2 Software Development](#52-software-development)
  - [5.3 Build, Tooling and Platform Notes](#53-build-tooling-and-platform-notes)
  - [5.4 Conclusions](#54-conclusions)
- [6. Testing and Evaluation](#6-testing-and-evaluation)
  - [6.1 Introduction](#61-introduction)
  - [6.2 System Testing](#62-system-testing)
  - [6.3 System Evaluation](#63-system-evaluation)
    - [6.3.1 Message Delivery](#631-message-delivery)
    - [6.3.2 Holder Supply](#632-holder-supply)
    - [6.3.3 The Distributed Hash Table Is Not the Bottleneck](#633-the-distributed-hash-table-is-not-the-bottleneck)
    - [6.3.4 Real-Time Exchange, Battery, and Throughput](#634-real-time-exchange-battery-and-throughput)
    - [6.3.5 Metadata Privacy](#635-metadata-privacy)
    - [6.3.6 Adversarial Scenarios](#636-adversarial-scenarios)
    - [6.3.7 Honesty Layer](#637-honesty-layer)
  - [6.4 Evaluation of Project Management](#64-evaluation-of-project-management)
    - [6.4.1 Evaluation of the Project Plan](#641-evaluation-of-the-project-plan)
    - [6.4.2 Evaluation of Project Execution](#642-evaluation-of-project-execution)
  - [6.5 Conclusions](#65-conclusions)
- [7. Conclusions and Future Work](#7-conclusions-and-future-work)
  - [7.1 Introduction](#71-introduction)
  - [7.2 Future Work](#72-future-work)
  - [7.3 Conclusions](#73-conclusions)
- [References](#references)
- [Appendix A: Prompts Used with Gen AI](#appendix-a-prompts-used-with-gen-ai)
  - [C.1 Prompts related to Report Writing](#c1-prompts-related-to-report-writing)
  - [C.2 Prompts Related to Research](#c2-prompts-related-to-research)
  - [C.3 Prompts Related to Design](#c3-prompts-related-to-design)
  - [C.4 Prompts Related to Coding](#c4-prompts-related-to-coding)

## List of Figures and Tables

### Figures

- Figure 1: Aura layered architecture, instantiated as a dating application. Every device runs the entire stack; there is no operator-run server.
- Figure 2: Message delivery path: a direct encrypted stream when the recipient is online and reachable, otherwise store-and-forward via peer mailbox holders.
- Figure 3: The verifying profile-fetch cascade. Each hop checks the CID against the bytes and verifies the owner's P-256 signature.
- Figure 4: End-to-end message delivery under the modelled Irish scenario, by rendezvous assumption and user archetype.
- Figure 5: Delivery as a function of reachable-holder supply (conservative gate). Capacity saturates near 84 percent; the supply of reachable serving nodes is the binding constraint.
- Figure 6: Measured DHT rendezvous recall at scale on the real go-libp2p Kademlia (structural upper bound under idealised transport). Record findability is robust; holder survival is the variable.
- Figure 7: Measured replication requirement. Raising the default from three to five holders sustains heavy churn (improves holder-survival recall).
- Figure 8: Direct real-time connection probability by NAT pairing (calibrated). Hard-NAT mobile pairs rarely connect without a relay.
- Figure 9: Modelled background battery cost per day by configuration. The adaptive default is approximately 9 percent per day.
- Figure 10: Holder throughput. Photographs, not mailbox messages, are the serving wall; compression relaxes it roughly four-fold.

### Tables

- Table 1: Comparison of decentralised social systems. The decisive axis is stranger discovery with no central server.
- Table 2: Base-level capability requirements by social-application class. Dating is heavier on discovery, proximity, matching, and safety, and lighter on broadcast, feed ranking, and content growth.
- Table 3: The four architecture decision records that fix the design.
- Table 4: Whole-application headline scorecard, with source tags (measured / modelled / calibrated) and bias signs ((+) optimistic, (?) unknown).
- Table 5: The testing ladder. Scale comes from simulation; realism comes from the real stack; the emulator confirms protocol correctness, not quantitative rates.
- Table 6: Delivery as a function of the rendezvousConsistency parameter (modelled sweep, Layer-A). The honest claim is the curve, not a single figure.
- Table 7: Path decomposition of end-to-end delivery (modelled). The mailbox is the workhorse; the direct real-time path contributes a small remainder.
- Table 8: Structural metadata-exposure comparison (analytic, protocol-derived). Aura confines exposure to ephemeral, partial intermediaries rather than a single persistent operator.
- Table 9: Adversarial scenarios. Every run confirms that reachable holder supply is the governing variable.

## 1. Introduction

### 1.1 Project Background

Under the blanket term “social network” there are lots of distinct platforms that have become the primary medium of online interaction, and with that role has come an unprecedented concentration of personal data inside a small number of operators. The concentration is significant and noticeable: a handful of firms dominate online attention and activity [12], and the dominant business model monetises behavioural data, even though users, given a genuine choice, place a measurable value on data ownership and prefer tracking-free alternatives [9, 10, 11]. Dating is the sharpest instance of this problem, and a useful lens on it, because the data involved is the most intimate and the consequences of its exposure the most severe. The global dating market alone reached an estimated 6.18 to 9.07 billion US dollars in 2024, serving on the order of 350 to 364 million users worldwide [1], and it treats the user's most sensitive disclosures - sexual orientation, health status, precise location, and the full graph of who they are attracted to - as a monetizable asset.

The Mozilla Foundation's 2024 privacy audit found that 88 percent of the dating applications it examined (22 of 25) earned a "Privacy Not Included" warning, the worst category result in its consumer technology reviews [2]. The risks are not hypothetical. Grindr was documented sharing the HIV status of its users with third-party analytics firms [3], and the 2015 Ashley Madison breach exposed the intimate records of roughly 37 million users, with reporting later linking the fallout to suicides [4, 5]. Academic work characterises the resulting tension as a structural property of the platform model rather than an accident of implementation [6, 7, 8], and the GDPR principle of data protection by design and by default [13] is difficult to satisfy under an architecture whose revenue depends on data collection.

These failures have repeatedly motivated decentralised alternatives across the whole family of social software, yet none has displaced the incumbents, and the pattern of where they succeed and where they stop is instructive. Briar achieves strong security through a pure peer-to-peer design but assumes contacts are exchanged manually and out of band, so it cannot introduce strangers [16]. Secure Scuttlebutt provides an offline-first append-only log with attractive identity properties [17] but exhibits unbounded storage growth and scaling difficulties, and likewise has no mechanism for meeting people one has never met [18, 19]. Bluesky and the AT Protocol, Mastodon, and Nostr show that publishing and identity can be decentralised while preserving usability [14, 15, 35], but they discover people and content through centralised relays, indexes, or application views. Solid reclaims data ownership through personal data stores [20] but still presupposes hosted infrastructure. The recurring lesson is that the hard, unsolved problem for decentralised social software is not interaction among known contacts, nor publishing; it is the discovery of strangers without a central index to broker it.

Dating is therefore the most demanding test case for decentralised social software, because stranger discovery is its core function rather than an optional feature, and it is the case this project uses to stress-test a general architecture. The remainder of this report builds a peer-to-peer substrate for social applications, instantiates it as a dating application, and measures how far it can be pushed before the realities of consumer mobile networks force a compromise.

### 1.2 Project Description

This project develops Aura, a peer-to-peer architecture for mobile social applications in which there is no operator-run server, instantiated and evaluated as a dating application. Each device is simultaneously the authoritative store of its owner's data and a cooperative participant in the network that carries other users' data. The application is built in Kotlin Multiplatform, with Android as the primary target and an iOS code path scaffolded behind the shared interfaces but not yet implemented at the cryptographic and transport layers.

Rather than adopting the server software of an existing decentralised protocol, Aura reimplements the data model of the AT Protocol natively: each record (profile, like, match, message) is canonically encoded as deterministic DAG-CBOR, content-addressed with a CIDv1, and signed by the owner's identity key, with the records summarised by a signed commit [15, 28]. Networking is provided by go-libp2p, embedded via gomobile, exposing a narrow byte-oriented boundary so that all cryptography, encoding, and policy remain in auditable Kotlin [34]. Discovery combines a project-private Kademlia distributed hash table with GossipSub, both keyed by geohash, so that a peer can find others nearby without consulting any central index [22]. Messaging is end-to-end encrypted, and messages for offline recipients are stored and forwarded by ordinary peers acting as mailbox holders, so that asynchronous delivery requires no server. This substrate is application-agnostic; the dating application is the instantiation through which it is exercised. The overall structure is shown in Figure 1.

![Figure 1: Aura layered architecture, instantiated as a dating application](images/figure-1.png)

_Figure 1: Aura layered architecture, instantiated as a dating application. Every device runs the entire stack; there is no operator-run server._

### 1.3 Project Aims and Objectives

The overall aim of this project is to determine how much of a mobile social application can be delivered with no central server, using the most demanding class of such application, dating, as the stress test, and to measure rather than assert where the limits lie. The research question is stated as follows: can a decentralised social application discover strangers, not merely connect known contacts, with no central server, and if not completely, how close, and at what cost in latency, availability, and metadata privacy? Dating, in which stranger discovery is the core function rather than an add-on, is used as the demanding test case.

The specific objectives are:

1. Implement a self-sovereign identity system using the did:key method, with deterministic recovery from a mnemonic seed phrase and no registration authority.

2. Reimplement an AT-Protocol-inspired on-device repository of signed, content-addressed records (DAG-CBOR, CIDv1, signed commits) suitable for any social application, without any server runtime.

3. Provide discovery of nearby strangers with no central index, using a project-private Kademlia DHT and GossipSub keyed by geohash.

4. Provide end-to-end encrypted messaging with asynchronous delivery via peer mailbox holders, requiring no server.

5. Build a cooperative relay-cache layer that lets records remain reachable while their owners are offline, with explicit abuse defences.

6. Characterise the base-level capability kit of a dating application relative to other decentralised social-application classes (messaging, microblogging, append-only social), identifying where it is heavier and where it is lighter.

7. Evaluate the system at scale through a calibrated simulation testbed, reporting delivery rate, battery cost, metadata exposure, and the structural limits of the design with an explicit measured-versus-modelled honesty layer.

### 1.4 Project Scope

Aura is scoped both as a general substrate and as a case study. In scope: a peer-to-peer substrate for social applications (identity, signed records, discovery, relay, and messaging), instantiated as an Android-first dating application with text messaging and simple deterministic match filtering on age, distance, gender, and interests. The substrate is application-agnostic and would equally support a broadcast-style or local-community social application; dating is chosen as the instantiation because it is the hardest case for the discovery dimension. Out of scope: a production iOS build (the iOS actuals for cryptography and transport are deliberately left as stubs); any blockchain component; machine-learning recommendation; voice or video; and any claim of operating with no infrastructure at all. The sole external dependency is the public IPFS DHT, used transiently and optionally as a last-resort cold-start rendezvous and replaceable by a user-supplied bootstrap peer; the project therefore claims no operator-run server, not the absence of all infrastructure. The honest frame, established here and revisited in the evaluation, is that it measures how much of a social application can run with no central server and where the inherent floors lie.

### 1.5 Thesis Roadmap

Chapter 2 reviews existing decentralised social systems and the building-block technologies, establishing that the components exist but their composition for stranger discovery is unsolved. Chapter 3 situates dating among the classes of social application, derives a four-layer logical architecture, and identifies the binding constraint that predicts the system's limits. Chapter 4 presents the design, organised around four architecture decision records. Chapter 5 describes the implementation, including the genuine engineering obstacles encountered. Chapter 6 evaluates the system through a calibrated simulation testbed and answers the research question with data. Chapter 7 concludes and sets out the future work.

## 2. Literature Review

### 2.1 Introduction

This chapter is organised around a single question: which parts of decentralised social software, and of stranger discovery in particular, are already solved by existing systems and research, and which are not. It reviews the most relevant decentralised social systems, then the building-block technologies on which Aura draws, and concludes that the necessary components exist individually but have not been composed into a system that can introduce strangers with no central server.

### 2.2 Alternative Existing Solutions

Five decentralised social systems bound the design space. Briar is a pure peer-to-peer messenger whose security properties are excellent, but contact exchange is manual and out of band, so it cannot introduce strangers and is not intended to [16]. Secure Scuttlebutt is an identity-centric append-only-log protocol with strong offline-first behaviour [17], but append-only logs grow without bound and replication strains beyond a modest network size [18, 19], and, like Briar, it offers no stranger discovery. Bluesky and the AT Protocol show that a hybrid of decentralised identity and data with centralised discovery can retain mainstream usability [14], and its repository, DID, and lexicon model [15] directly inspired Aura's data layer; the cost is that the live network depends on centralised relays and application views. Mastodon's federated model and Nostr's relay model [35] decentralise publishing but discover people and content through servers, and Solid's personal data stores [20] retain hosted infrastructure. Table 1 summarises the comparison along the axes that matter for this project; the decisive column is stranger discovery, where every existing system either declines the problem or solves it with a server.

_Table 1: Comparison of decentralised social systems. The decisive axis is stranger discovery with no central server._

|System|Stranger discovery|Server required|E2E encryption|What Aura takes / avoids|
|---|---|---|---|---|
|Briar|No (manual contacts)|No|Yes|Takes: pure-P2P security. Avoids: contact-only discovery.|
|Scuttlebutt|No (friend-of-friend)|Partial (pubs)|Per-message|Takes: signed append-only identity. Avoids: unbounded log growth.|
|Bluesky / AT Proto|Yes (relay/AppView)|Yes|No (public)|Takes: signed content-addressed repo, did:key. Avoids: hosted relays.|
|Mastodon / Nostr|Yes (instances/relays)|Yes|DM only|Takes: simple signed events. Avoids: relay/instance servers.|
|Aura (this work)|Yes (geohash DHT + gossip)|No|Yes (ECIES)|-|

### 2.3 Technologies Researched

The building blocks are mature individually. The Kademlia distributed hash table provides O(log n) lookup and is the basis of Aura's discovery and provider-record rendezvous [22]. go-libp2p supplies a production implementation of Kademlia, GossipSub, the Noise channel, QUIC and TCP transports, AutoNAT, DCUtR hole-punching, and Circuit Relay v2 [34]. Deterministic DAG-CBOR and CIDv1 give canonical, content-addressed, signable records [28]. NAT traversal has been studied extensively: the classic STUN/TURN/ICE stack [24] and the more recent measurement of libp2p's DCUtR, which reports roughly 70 percent hole-punch success across some 4.4 million attempts using public relays [25], together with the broad consensus that two hard NATs cannot be punched without a relay [26]. On energy, Kassinen and colleagues measured the battery cost of participating in a Kademlia overlay on mobile [29], and Huang and colleagues characterised the LTE radio power model that underpins this project's battery model [30]. Irish-specific connectivity figures are taken from Ookla's 2025 report [31] and IPv6 adoption data [23]. Finally, the embedded-Node path explored early in the project [32, 33] is reviewed as a rejected option, for reasons set out in Chapter 4 and reflected upon in Chapter 6.

### 2.4 Other Relevant Research

The Signal protocol's X3DH and Double Ratchet define the gold standard for end-to-end encryption and forward secrecy [27], and provide the benchmark against which Aura's lighter ECIES scheme is honestly compared in Chapter 4. The gossip and epidemic-broadcast literature [21] informs the GossipSub-based presence layer, and the AT Protocol's repository design [15] informs the on-device store.

### 2.5 Existing Final Year Projects

Prior final-year work in the decentralised and location-aware space typically either assumes a coordinating server or restricts itself to known-contact messaging. Aura is differentiated by attacking the stranger-discovery problem under a strict no-server constraint and by quantifying the limits of doing so.

### 2.6 Conclusions

The components required for stranger-discovery social software all exist: content-addressed signed records, a distributed hash table, a gossip layer, NAT traversal, and end-to-end encryption. What does not exist is their composition into a system that can introduce strangers with no central server and survive the realities of consumer mobile networks. That composition, and the measurement of where it succeeds and where it is bounded, is the contribution of this project.

## 3. System Analysis

### 3.1 System Overview

From the user's perspective Aura behaves like a conventional dating application: the user creates a profile, browses a feed of nearby candidates, likes or passes, forms a match when a like is reciprocated, and exchanges messages. The decentralisation is deliberately invisible. The single behavioural difference is that delivery is best-effort rather than instantaneous: a message to an offline user is held by the network and delivered when that user next comes online, in the manner of email rather than an always-connected chat server.

### 3.2 The Base-Level Kit: Dating Among Decentralised Social Applications

Before specifying Aura's requirements it is worth situating a dating application within the broader family of social applications, because the choice of dating as the case study is deliberate and the contrast is instructive. Different classes of social application demand different base-level capabilities from a decentralised substrate, and a dating application is not uniformly more or less demanding than the others; it is heavier on some axes and markedly lighter on others. Table 2 sets out the comparison across four representative classes: private messaging (exemplified by Briar), microblogging or broadcast (Bluesky, Mastodon, Nostr), append-only social (Secure Scuttlebutt), and dating (Aura).

_Table 2: Base-level capability requirements by social-application class. Dating is heavier on discovery, proximity, matching, and safety, and lighter on broadcast, feed ranking, and content growth._

|Capability|Messaging (Briar)|Microblog (Bluesky/Mastodon/Nostr)|Append-log (Scuttlebutt)|Dating (Aura)|
|---|---|---|---|---|
|Stranger discovery|Not needed (manual contacts)|Needed, usually via central relay/index|Not needed (friend-of-friend)|Core, with no central index (the hard part)|
|Proximity / location|No|Optional|No|Yes (geohash)|
|Mutual-consent matching|No|No (follow is unilateral)|No|Yes|
|Real-time 1:1 messaging|Core|Secondary (DMs)|No (async)|Yes|
|Offline store-and-forward|Yes (mailbox)|Pull-based feeds|Yes (by design)|Yes|
|End-to-end encryption|Yes|No (public content)|Signed, not encrypted|Yes|
|Public broadcast / fan-out|No|Yes, heavy|Yes (gossip replication)|No|
|Global feed / ranking|No|Yes, heavy|No|No (local bounded feed)|
|Content growth|Low|Unbounded posts|Unbounded logs (bloat)|Bounded (profile + few photos + ephemeral chat)|
|Abuse / safety sensitivity|Lower (closed contacts)|High (spam / disinformation)|Medium|High (culminates in a physical meeting)|

Two patterns matter. First, dating is heavier than the others on the discovery side. Private messengers such as Briar assume contacts are exchanged out of band and so need no stranger discovery at all; append-only systems replicate among friends-of-friends; and even microblogging platforms, which do need people-and-content discovery, almost universally provide it through a central relay or index. Dating cannot: its entire purpose is to introduce people who have never met and have no shared contact, to do so with location relevance, and to do so behind a mutual-consent handshake in which a match is revealed only when both parties have independently expressed interest, without leaking unilateral likes. It is also the most safety-sensitive class, because a successful interaction culminates in a physical meeting. These requirements - stranger discovery with no central index, proximity, mutual-consent matching, and safety - are the heavy part of the kit, and three of the four are absent from every other class.

Second, and less obviously, dating is lighter than microblogging and gossip systems on precisely the dimensions that usually sink them. A dating application has no public broadcast: there is no one-to-many fan-out, no hot or celebrity accounts whose posts must reach millions, and no follower graph to maintain. Its content is bounded and largely ephemeral, comprising one current profile, a handful of photographs, and transient one-to-one conversations, rather than the ever-growing post history of a microblog or the unbounded append-only logs that cap Scuttlebutt's scalability [18]. There is no global timeline to rank; discovery is confined to a local geohash cell and a set of filters, which keeps the working set small. In short, dating trades the broadcast-and-storage scaling problem that dominates decentralised microblogging for the discovery-and-reachability problem that dominates dating.

This trade is exactly why dating is the right stress test for this project. It exercises the discovery and reachability dimensions to their limit, which are the genuinely unsolved problems for decentralised social software, while shedding the broadcast and storage burden that would otherwise confound the measurement. The empirical evaluation in Chapter 6 bears this out: storage per serving peer never exceeds about 1.4 megabytes and the system is never storage-bound, confirming the lightness of dating content, while the binding constraint is the supply of inbound-reachable peers for discovery and delivery, confirming where the weight lies. The corollary generalises beyond dating: any decentralised social application that requires stranger discovery, whatever its content model, inherits the reachability floor that Chapter 6 characterises, whereas broadcast-heavy applications instead inherit a fan-out and storage floor that dating avoids.

### 3.3 Requirements Gathering

Requirements were derived from two sources: the privacy and behavioural literature reviewed in Chapter 2, which establishes the problem and the user's latent demand for data ownership [6, 9, 11]; and a technical analysis of the constraints imposed by Android and by consumer mobile networks. No primary user study was conducted within the project timeframe; this is a genuine limitation and is owned as such. In consequence, the behavioural inputs used in the Chapter 6 evaluation (session timing, activity archetypes, weekly peaks) are assumption-based, parameterised from published dating-app usage data rather than from Aura's own users, and the requirements should be read as literature- and constraint-driven rather than as the product of fresh primary research. Because the application is privacy-preserving by construction, requirements were framed to avoid any design element that would require central data collection.

### 3.4 Requirements Analysis

The functional requirements follow directly from the heavy part of the kit identified in Section 3.2: self-sovereign identity with recovery; profile creation and signed publication; discovery of nearby strangers; deterministic filtering on age, distance, gender, and interests; like and mutual-match detection; end-to-end encrypted messaging; and offline message delivery. The principal nonfunctional requirements are: no operator-run server in the critical path; end-to-end confidentiality and integrity against any intermediary; an affordable background battery cost; and a delivery success rate high enough to be usable. The connection-success target inherited from the proposal is, in this report, deliberately reframed as a measured outcome with documented floors rather than a guarantee, a change justified by the evaluation in Chapter 6.

### 3.5 Logical Architecture of System

The requirements resolve cleanly into four layers: (1) identity and the on-device repository; (2) transport; (3) discovery; and (4) relay and messaging. Each higher layer depends only on the one below it, and each is designed to push toward zero infrastructure. Figure 1 in Chapter 1 shows the physical realisation, in which every device instantiates all four layers.

### 3.6 Initial System Specification

The specification fixes the following: identity is a P-256 did:key signing key plus a separate Ed25519 transport key; records are deterministic DAG-CBOR addressed by CIDv1 and signed with ECDSA; transport is go-libp2p; discovery is geohash-keyed DHT plus GossipSub; messaging is P-256 ECIES with a peer-mailbox fallback; and storage is a Room database split into a personal repository and a derived application view. The rationale for each choice is the subject of Chapter 4.

### 3.7 Conclusions

The binding constraint of the entire design is the all-smartphone worst case: every participant is a mobile device that is frequently behind carrier-grade NAT, on a rotating address, and asleep under the Android Doze power regime. This single constraint predicts in advance where the system's floors will appear, namely in the supply of inbound-reachable serving nodes and in real-time reachability between two mobile peers, and, per Section 3.2, this is a floor that any decentralised social application requiring stranger discovery would share. Chapter 6 confirms the prediction quantitatively.

## 4. System / Experiment Design

### 4.1 Introduction

This chapter presents the design as a sequence of justified decisions. Every major decision is recorded as an architecture decision record (ADR), and each is justified by the same criterion: push toward zero infrastructure while preserving privacy. The four ADRs are summarised in Table 3 and elaborated in Section 4.5; the detailed designs for messaging, the relay cache, and background operation follow in Section 4.6.

### 4.2 Software Methodology

Development followed an incremental, phased method. The system was built in numbered phases (identity and records, then transport, then discovery, then fetch, then relay and messaging), and each phase ended in an acceptance checkpoint verified on Android emulators using a gated debug harness. The codebase is a single Kotlin Multiplatform module with platform-specific behaviour isolated behind expect/actual seams, so that the Android implementation could proceed while the iOS path remained a compilable stub.

### 4.3 Project Planning

The project was planned in three externally-visible phases: proposal, interim, and final development. The plan was revised once, significantly, after an early implementation approach proved unworkable; that revision and its lessons are discussed in Section 6.4 rather than here, to keep the design narrative clean.

#### 4.3.1 Project Schedule

The detailed schedule and Gantt chart track the phased plan above. The networking phases (transport, discovery, fetch) were the critical path, since every later feature depends on them.

### 4.4 Overview of System

The logical and physical architecture is as shown in Figure 1: four layers, instantiated on every device, with no server. The personal half of the on-device database holds the user's own signed records and commits; the application-view half holds a derived, cached index of other peers together with the relay cache and the mailbox. Writes flow through a repository manager that canonically encodes, hashes, signs, and commits; reads for the feed flow through discovery and a verifying fetch cascade.

#### 4.4.1 Fulfilment of System Requirements

Each functional requirement maps to a specific design element: identity and recovery to the P-256 did:key and seed derivation; signed publication to the DAG-CBOR/CID/commit pipeline; discovery to the geohash DHT and GossipSub; filtering to the deterministic discovery filters; messaging and offline delivery to ECIES and the mailbox; and reachability of offline records to the relay cache. The requirements deferred (a production iOS build, forward secrecy, enforced blocking, and active gossip-mesh hardening) are named honestly in Chapter 7.

### 4.5 Design Decisions: the Four ADRs

_Table 3: The four architecture decision records that fix the design._

|ADR|Decision|Chosen over|Principal reason|
|---|---|---|---|
|0001 Messaging|P-256 ECIES (ephemeral ECDH + HKDF + AES-GCM)|libsignal (X3DH + Double Ratchet)|libsignal is not Kotlin Multiplatform, is AGPL, is unsupported outside Signal, and is heavy to build|
|0002 Connectivity|Geohash DHT + GossipSub; IPv6-first, DCUtR, Circuit Relay v2; no hosted TURN|Centralised discovery / hosted relays|Keeps presence off any central index and keeps infrastructure at zero|
|0003 Key custody|Split P-256 signing (did:key) + Ed25519 transport; seed-derived, encrypted at rest|Single key; or hardware-non-exportable key|P-256 is Keystore- and ATProto-valid; recovery requires a derivable key|
|0004 libp2p runtime|go-libp2p via gomobile|jvm-libp2p; rust-libp2p|jvm-libp2p lacks Kad-DHT, rendezvous, bootstrap, and DCUtR|

Each ADR is presented in full in Appendix B as a problem, the options considered, the decision, and the consequences. The consequence sections name the cost that each decision imposes toward the system's floor, which is what threads the design into the evaluation. Beyond the four ADRs, the design fixes the canonical DAG-CBOR encoding rules, the discovery keys and topics (a five-character geohash both as the DHT key and as the GossipSub topic, with subscription to the eight neighbouring cells), and the five-defence relay cache described in Section 4.6.

### 4.6 Detailed Design Topics

#### 4.6.1 Messaging and End-to-End Encryption

Messages are sealed with a P-256 ECIES construction: the sender performs an ephemeral elliptic-curve Diffie-Hellman against the recipient's long-term P-256 identity point, which is already published as the recipient's did:key, derives a symmetric key with HKDF-SHA256, and encrypts the body with AES-256-GCM. The wire payload is the ephemeral public key, the nonce, and the ciphertext, with the AEAD associated data binding the sender and recipient identifiers so a ciphertext cannot be re-targeted. Each envelope additionally carries the sender's ECDSA signature for authentication. libsignal was rejected for the reasons in ADR-0001; the accepted cost, stated honestly, is that ECIES sealing to a static identity key does not provide the forward secrecy of the Double Ratchet [27], and that a single P-256 key is currently reused for both signing and key agreement. Both limitations are isolated behind a single interface so that a ratchet can be added later. The delivery path is shown in Figure 2.

![Figure 2: Message delivery path: a direct encrypted stream when the recipient is online and reachable, otherwise store-and-forward via peer mailbox holders](images/figure-2.png)

_Figure 2: Message delivery path: a direct encrypted stream when the recipient is online and reachable, otherwise store-and-forward via peer mailbox holders._

#### 4.6.2 Discovery, the Fetch Cascade, and Abuse Posture

A peer announces a signed presence record under a DHT key derived from its five-character geohash, and republishes it to the corresponding GossipSub topic; discovery merges a one-shot DHT lookup with the continuous gossip stream, verifies each record's signature, and applies the user's filters client-side. A discovered identifier is resolved to a full profile through the four-step cascade in Figure 3, which verifies the content identifier and the owner's signature at every hop, so that an intermediary can withhold data but can never forge it.

![Figure 3: The verifying profile-fetch cascade](images/figure-3.png)

_Figure 3: The verifying profile-fetch cascade. Each hop checks the CID against the bytes and verifies the owner's P-256 signature._

On abuse resistance, the cryptographic layer is strong but the mesh layer is not yet hardened, and this is stated plainly. Because every presence and profile record is signed and content-addressed, a malicious gossip peer can flood a topic or attempt an eclipse but cannot forge or alter a record. However, the topic-level defences that would resist flooding and eclipse on the public per-geohash presence topics, namely GossipSub peer scoring, access gating, and message validators, are implemented but disabled by default pending parameter validation. The discovery mesh therefore ships without active abuse hardening, which is recorded as a known limitation and as deferred work in Chapter 7 rather than presented as solved.

#### 4.6.3 The Relay Cache and Mailbox

Because there is no server, ordinary peers cache records they have seen and hold messages for offline recipients. Ingestion into the relay cache is governed by five defences: a non-self check; a dual-rate token-bucket limiter; a one-shot session interaction token minted only when a profile card actually renders, which prevents headless bulk harvesting; a capacity cap with least-recently-served eviction; and authenticated encryption at rest with the owner identifier and content identifier bound into the associated data. The mailbox reuses the same holder role: a sealed message is deposited with several holders located through DHT provider records, held under a seven-day time-to-live, and released only to a caller who proves control of the recipient identifier by signing a holder-issued challenge. The residual exposure, that a holder learns the sender, recipient, time, and size of a message it accepts, is documented as a metadata floor rather than hidden, and is quantified in Section 6.3.

#### 4.6.4 Background Operation, Bootstrap, and Connectivity Posture

A serverless mobile design faces a hard constraint: a suspended Android application under Doze holds no sockets and can neither receive a message nor serve as a holder. The design adopts a two-tier model with local notifications only: an opt-in foreground service for users who want instant delivery and to contribute capacity, bounded by the Android 15 six-hour daily limit on data-sync foreground services; and, by default, periodic cooperative serve windows on a duty cycle. Cold-start bootstrap uses a dynamic self-building peerstore, falling back to the public IPFS DHT only as a transient, last-resort rendezvous to find a first peer; this is the single external dependency, is optional and user-replaceable, and is the reason the project claims no operator-run server rather than no infrastructure. On connectivity, AutoNAT and DCUtR are enabled but AutoRelay is deliberately not, because actively advertising relay reservations would route traffic through third parties and expose a stable transport-identity-to-address linkage; for a dating application this metadata exposure is unacceptable, so reachability rests on IPv6-direct and hole-punching, with a trusted relay tier wired but disabled until a relay set is deployed.

## 5. System / Experiment Development

### 5.1 Introduction

The application is implemented in Kotlin Multiplatform using Compose Multiplatform for the user interface, Decompose for navigation, MVIKotlin for state, Koin for dependency injection, Room for storage, and Coil for image loading. Networking is go-libp2p (v0.48.0) compiled to an Android archive via gomobile [34]. This chapter describes the implementation layer by layer and the genuine obstacles encountered.

### 5.2 Software Development

The identity and records layer implements the P-256 did:key derivation, the deterministic DAG-CBOR encoder and CIDv1 codec, and the repository manager that signs and commits records. The transport layer is a Go wrapper exposing a narrow byte-oriented API, compiled with gomobile and called through a Java Native Interface shim; all encoding, signing, and policy remain in Kotlin. On top of transport sit the discovery service (geohash presence over DHT and GossipSub), the verifying fetch cascade, the five-defence relay cache, the ECIES messaging service with delivery receipts, the mailbox, like and match handling, the Aura user interface, and the background serve-window and foreground-service machinery.

The single most instructive engineering obstacle was a platform-level discovery failure. go-libp2p's multicast-DNS peer discovery does not function on Android, because the platform's SELinux policy denies ordinary applications the ability to bind a netlink route socket (Android Open Source Project issue b/155595000); as a consequence the host enumerates only its loopback address. This was diagnosed from first principles and worked around by gating off the library's multicast-DNS and performing local-network discovery through Android's own NsdManager service, with the device address obtained from ConnectivityManager. This is a genuine integration finding, discovered by encountering it rather than read from documentation, and it is reported as such.

### 5.3 Build, Tooling and Platform Notes

The Go runtime is bound with gomobile to a per-architecture archive (arm64, arm, and amd64), which is extracted into a Java archive of classes and a set of native shared objects packaged with the application. The expect/actual seams (secure key storage, AEAD, key agreement, transport, location, Bluetooth, network service discovery, and background execution) localise all platform code; the iOS actuals are fail-loud stubs. The principal cost of embedding a full libp2p runtime is binary size: the debug application is on the order of 150 megabytes, and the Go runtime is not removable by the Android shrinker.

### 5.4 Conclusions

At the close of development the core loop, comprising identity, discovery, verified profile fetch, matching, and encrypted messaging with offline delivery, runs end to end on Android with no server. The question that remains, and that the next chapter answers, is whether it works at scale and where its limits lie.

## 6. Testing and Evaluation

### 6.1 Introduction

Evaluating a peer-to-peer system with no central server at realistic scale is itself a research problem: the more faithful the thing one runs, the fewer instances one can run. The evaluation therefore uses a testing ladder, trading fidelity against scale, and governs it with a strict honesty discipline in which every quantitative claim carries a source tag (measured, modelled, or calibrated) and a bias sign indicating whether the real world is likely to be better or worse. No claim is treated as stronger than the weakest idealisation feeding it. To instantiate that discipline rather than merely describe it, the headline scorecard in Table 4 carries its own source and bias columns.

_Table 4: Whole-application headline scorecard, with source tags (measured / modelled / calibrated) and bias signs ((+) optimistic, (?) unknown)._

|Dimension|Result|Source|Bias|Verdict|
|---|---|---|---|---|
|End-to-end message delivery|~84% (rdz 0.85) to ~93% (rdz 0.95)|model|(+)|Viable, with a reachable tier|
|Default battery cost|~9% per day (adaptive cadence)|model|(+)|Affordable|
|Real-time (both peers on mobile)|~19% (CGNAT-to-CGNAT ~10%)|calib|(?)|Weak; needs a relay tier|
|DHT rendezvous recall|~100% at N=1000 (structural upper bound, idealised transport)|meas|(+)|Not the bottleneck|
|Storage per holder|~1.4 MB at the busiest|model|(?)|Non-issue (dating content is light)|
|Photo throughput per holder|~35 uncompressed; ~143 compressed / 5-min cellular window|model|(+)|Was the wall; compression relaxed it|

The one-line conclusion is that delivery with no central server is viable and battery-affordable, but it is gated almost entirely by the supply of inbound-reachable serving nodes, estimated at roughly 17 percent of devices in Ireland, and not by the distributed hash table. The only genuinely weak dimension is real-time exchange when both peers are on mobile networks.

### 6.2 System Testing

Correctness is established by unit tests over the canonical encoder, the content-identifier codec, geohash arithmetic, the did:key identity, the ECIES round trip, and the five relay-cache defences, using in-memory fakes. End-to-end behaviour is exercised on two Android emulators through a gated debug harness whose roles (message, match, and invalidation checks) all pass. It is important to be precise about what these emulator runs do and do not establish: they run over loopback with no network address translation, and they confirm that the message, match, and invalidation protocols round-trip correctly. They are therefore a functional confirmation of protocol correctness, not a quantitative calibration anchor; the delivery, NAT-pair, and battery figures reported below are modelled and literature-calibrated, and are not derived from the emulator runs. The testing ladder is summarised in Table 5.

_Table 5: The testing ladder. Scale comes from simulation; realism comes from the real stack; the emulator confirms protocol correctness, not quantitative rates._

|Layer|What it runs|Trust it for|Never trust it for|
|---|---|---|---|
|Unit|pure logic + fakes|function correctness|system-level numbers|
|L-A in-JVM|real Aura services + virtual clock|relative ordering, sweeps, recall logic|absolute transport/DHT/DB numbers|
|L-B mocknet (Go)|real go-libp2p DHT/gossip|protocol behaviour, structural rendezvous at scale|transport-failure realism|
|L-B netns/tc|real binaries + NAT + netem|NAT-pair effects, loss/latency|thousands-of-nodes scale|
|Emulator|real Android app, a few nodes|end-to-end protocol correctness (functional confirmation)|scale, churn, geography, any quantitative rate|

### 6.3 System Evaluation

#### 6.3.1 Message Delivery

The integrating metric is end-to-end message delivery, because it folds together user behaviour, network address translation, discovery, and holder availability. Under the modelled Irish scenario (3000 users over a full week, 20000 messages), the base delivery rate is approximately 84 percent on the conservative rendezvous assumption and approximately 93 percent on the measurement-backed assumption, with a median latency of about 18 minutes and a ninetieth-percentile latency of about 116 minutes (Figures 4 and 5).

![Figure 4: End-to-end message delivery under the modelled Irish scenario, by rendezvous assumption and user archetype](images/figure-4.png)

_Figure 4: End-to-end message delivery under the modelled Irish scenario, by rendezvous assumption and user archetype._

The headline range rests almost entirely on one parameter, the rendezvousConsistency term, and the honest presentation is therefore the full sweep (Table 6) rather than a single endpoint. The conservative floor of 0.85 corresponds to about 84 percent delivery; the measurement-backed central estimate of 0.95 corresponds to about 93 percent. The justification for moving from 0.85 to 0.95 is given in Section 6.3.3: the at-scale DHT measurement shows that record findability alone is essentially 100 percent, so the conservative 0.85 was double-counting holder churn that the model already penalises mechanistically. The value is not pushed higher than 0.95 because the mocknet idealises the lookup transport and the expiry of provider records over gaps longer than 48 hours remains unmeasured; 0.85 is therefore retained as a conservative floor, 0.95 as the central estimate, and 0.90 (about 89 percent) as a defensible lower-central alternative.

_Table 6: Delivery as a function of the rendezvousConsistency parameter (modelled sweep, Layer-A). The honest claim is the curve, not a single figure._

|rendezvousConsistency|1.00|0.95|0.90|0.80|0.60|
|---|---|---|---|---|---|
|Delivery success (%)|97.4|92.9|88.6|79.1|60.6|

The headline rate folds two delivery paths into a single figure, and the split is worth surfacing because the proportions are uneven. In the modelled runs almost every successful delivery is carried by the peer mailbox; the direct real-time path contributes only a few percentage points (Table 7).

_Table 7: Path decomposition of end-to-end delivery (modelled). The mailbox is the workhorse; the direct real-time path contributes a small remainder._

|Delivery run|Total success|via mailbox|via direct real-time|Fail (mostly pull-fail)|
|---|---|---|---|---|
|Conservative (rendezvousConsistency 0.85)|83.6%|77.1 pp|6.5 pp|16.4%|
|Optimized (replication 5, adaptive cadence, rendezvousConsistency 0.95)|92.8%|84.8 pp|8.0 pp|7.2%|

Roughly 92 percent of all successful deliveries are carried by the peer mailbox; the direct real-time path contributes only about 6 to 8 percentage points. Conditioned on the mailbox actually being used, its success is about 82.5 percent on the conservative gate and about 92.2 percent on the measurement-backed gate. The isolated mailbox-only figure, taken from the dormant-recipient adversarial scenario in Section 6.3.6, is approximately 79 percent with reachable holders and approximately 6 percent without. The headline rates of approximately 84 and 93 percent are therefore, in effect, mailbox figures. This is not a weakening of the result but a sharpening of where the work is being done: the mailbox is the workhorse rather than the direct path, the binding constraint remains the supply of inbound-reachable holders on which the mailbox depends (Section 6.3.2), and the small direct-path contribution is the upper bound on what the unmeasured circuit-relay-v2 tier could displace if enabled (Section 6.3.4).

#### 6.3.2 Holder Supply

The dependence on holder supply is decisive, but, under the conservative gate, bounded. With no live holders, delivery collapses to about 6 percent, the real-time floor; with roughly fifteen reachable holders it recovers to about 82 percent; and it saturates near 84 percent, whether that capacity is supplied as many ordinary holders or as a single always-on node (Figure 6). Raising the mailbox replication factor from three to five sharply improves holder-survival recall under churn in the at-scale measurement (Figure 8), which is why the production default was raised; the end-to-end delivery rate itself barely moves with replication, precisely because it is gated by reachability rather than by record survival. The estimate that only about 17 percent of devices in Ireland are inbound-reachable is therefore the single most important number in the evaluation.

![Figure 5: Delivery as a function of reachable-holder supply (conservative gate)](images/figure-5.png)

_Figure 5: Delivery as a function of reachable-holder supply (conservative gate). Capacity saturates near 84 percent; the supply of reachable serving nodes is the binding constraint._

#### 6.3.3 The Distributed Hash Table Is Not the Bottleneck

Measured on the real go-libp2p Kademlia implementation at scale (in-memory, up to 1000 nodes, with behavioural churn), the probability that a deposited provider record can still be found is essentially 100 percent even at the largest scale tested (Figure 7). This figure must be read with its qualifier: it is a structural upper bound obtained under an idealised transport, because the mocknet has no dial timeouts and no network address translation. The real-world rendezvous value is this structural recall multiplied by transport reachability, and is therefore lower in the field. What the measurement does establish, unambiguously, is that record findability is not the loss term; the losses live in holder survival, which replication addresses (Figure 8), and in reachability, which it does not. This is what moved the binding constraint off the DHT and onto holder supply and reachability, and what justified the rendezvous-term correction in Section 6.3.1.

![Figure 6: Measured DHT rendezvous recall at scale on the real go-libp2p Kademlia (structural upper bound under idealised transport)](images/figure-6.png)

_Figure 6: Measured DHT rendezvous recall at scale on the real go-libp2p Kademlia (structural upper bound under idealised transport). Record findability is robust; holder survival is the variable._

![Figure 7: Measured replication requirement](images/figure-7.png)

_Figure 7: Measured replication requirement. Raising the default from three to five holders sustains heavy churn (improves holder-survival recall)._

#### 6.3.4 Real-Time Exchange, Battery, and Throughput

Real-time exchange, in which both peers are simultaneously online, is weak and is the design's genuine limitation. Calibrated against the NAT-traversal literature [24, 25, 26], a realistic cellular pair connects directly only about 19 percent of the time, and two carrier-grade-NAT peers on different carriers only about 10 percent (Figure 9); roughly four in five online-to-online sends therefore still fall back to the mailbox. This is not a defect of the implementation but a property of consumer mobile networks, and it is the reason the design wires in, though does not yet enable, a trusted relay tier.

![Figure 8: Direct real-time connection probability by NAT pairing (calibrated)](images/figure-8.png)

_Figure 8: Direct real-time connection probability by NAT pairing (calibrated). Hard-NAT mobile pairs rarely connect without a relay._

Battery cost was modelled with an event-driven radio model calibrated to the published LTE and WiFi power constants [29, 30]. At the adaptive serve cadence adopted as the default, the background cost is approximately 9 percent per day; a continuously-meshed foreground configuration would cost over 22 percent, and a naive always-on cellular configuration would be infeasible (Figure 10). The absolute percentages rest on literature constants rather than a real-device anchor and should be read as ordering and ratios; obtaining a real-device battery anchor is identified as open work. Holder throughput, not storage, is the resource wall: storage per holder peaks at only about 1.4 megabytes, but serving photographs is roughly sixteen times more expensive than serving mailbox messages, and uncompressed two-megabyte photographs saturate a holder's cellular serving window. Introducing photo compression on the production branch raises the number of full profiles servable in a five-minute cellular window from about 35 to about 143, and reduces a twenty-profile feed browse from about 48 megabytes to about 6 to 8 megabytes (Figure 11). The lightness of the content load, even with photographs, is a direct consequence of dating's place in the kit of Section 3.2: there is no broadcast fan-out to serve.

![Figure 9: Modelled background battery cost per day by configuration](images/figure-9.png)

_Figure 9: Modelled background battery cost per day by configuration. The adaptive default is approximately 9 percent per day._

![Figure 10: Holder throughput](images/figure-10.png)

_Figure 10: Holder throughput. Photographs, not mailbox messages, are the serving wall; compression relaxes it roughly four-fold._

#### 6.3.5 Metadata Privacy

Because the research question names metadata privacy as a cost to be evaluated, it must be assessed rather than merely asserted. Unlike delivery or battery, metadata exposure is a structural property of the protocol and is evaluated here analytically rather than by runtime measurement; the quantifiable contrast lies in the scope, the persistence, and the recipients of exposure. The comparison against a conventional centralised application is given in Table 8. The central quantitative statements are that message content is exposed to no intermediary at all in Aura (end-to-end encryption) against full server access in a centralised app; that routing metadata (sender, recipient, time, and size) is exposed only to the small set of mailbox holders actually used for the roughly 81 percent of sends that route through the mailbox, under a seven-day time-to-live, whereas a centralised operator observes 100 percent of messages permanently; and that the roughly 19 percent of sends that take the direct path expose nothing beyond the two endpoints and their transport-level addresses. Honesty requires stating the residual floor: Aura is not a sealed-sender system, so a holder does learn the metadata of the messages it accepts, and presence advertises a coarse five-kilometre geohash that is visible to other subscribers of that geohash topic. The design therefore improves markedly on the centralised baseline in content confidentiality, social-graph confidentiality, and persistence, but it does not reduce per-message routing metadata to zero, and that gap is the motivation for the sealed-sender work identified in Chapter 7.

_Table 8: Structural metadata-exposure comparison (analytic, protocol-derived). Aura confines exposure to ephemeral, partial intermediaries rather than a single persistent operator._

|What an intermediary learns|Aura (no central server)|Centralised application|
|---|---|---|
|Message content|Nothing (end-to-end encrypted)|Full plaintext access by the operator|
|Routing metadata (sender, recipient, time, size)|Only the few mailbox holders used, for the ~81% of sends that route via mailbox, under a 7-day TTL|The operator, for 100% of messages, retained indefinitely|
|Social graph|No single party holds it; reconstructible only by colluding across many holders|Held in full by the operator|
|Location|Coarse geohash (~5 km), visible to same-cell topic subscribers|Precise GPS, server-held|
|Identity|did:key pseudonym; transport identity is separate|Real identity, frequently with payment details|

#### 6.3.6 Adversarial Scenarios

Adversarial and negative scenarios confirm the analysis. Removing all live holders drops delivery to about 6 percent, and placing every holder behind carrier-grade NAT drops it to about 7 percent, demonstrating that reachability, not record-findability, is the binding constraint; conversely, the reachable holder tier rescues the hardest case, lifting delivery to dormant recipients from about 6 percent to about 79 percent (Table 9).

_Table 9: Adversarial scenarios. Every run confirms that reachable holder supply is the governing variable._

|Scenario|Result|Reading|
|---|---|---|
|No live holders|6.0% delivery|Collapses to the real-time floor|
|All holders behind CGNAT|7.25%|Reachability, not findability, binds|
|Extreme presence staleness|real-time 0.5%, delivery 82.3%|The mailbox absorbs a dead real-time path|
|Dormant recipients, with holders|79% vs 6% without|The reachable tier rescues the hardest case|

#### 6.3.7 Honesty Layer

The limits of the evaluation must be stated plainly. The DHT recall, the replication curves, and the end-to-end emulator passes are measured on the real stack; the DHT recall in particular is a structural upper bound obtained under an idealised transport. Delivery, battery, and throughput are modelled and lean optimistic, because real radio contention and dial-timeout realism are absent. The NAT-pair rates are calibrated from the literature, not measured, because a live hole-punch could not be triggered through the go-libp2p version's internals. The absolute battery percentage is unanchored against a real device. The behavioural inputs are drawn from published dating-app usage data rather than from Aura's own users. These caveats do not overturn the conclusion, but they bound it, and they define the open work in Chapter 7.

### 6.4 Evaluation of Project Management

#### 6.4.1 Evaluation of the Project Plan

The phased plan was sound and was largely followed; the networking phases correctly identified as the critical path consumed the majority of the effort. The principal deviations were the scope reductions made consciously and recorded honestly: a production iOS build, forward secrecy in messaging, enforced blocking, and active gossip-mesh hardening were deferred to keep the core loop and its evaluation complete rather than leaving several features half-built. The absence of a primary user study, noted in Section 3.3, is the most significant planning shortfall and is owned as such.

#### 6.4.2 Evaluation of Project Execution

The most important execution lesson concerns an early approach that was abandoned. The project initially attempted to run an existing Personal Data Server on the device itself by embedding a Node.js runtime and cross-compiling its native modules with the Android NDK [32, 33]. This collided with native-module architecture mismatches and, more fundamentally, contradicted the no-server goal, since the recommended remedy was to host the server elsewhere. The approach was abandoned in favour of reimplementing the data model natively in Kotlin. The lesson, carried forward, is to validate the riskiest dependency in isolation before building upon it; had the native-module build been spiked in the first week, the pivot would have come far sooner. This is recorded as a project-management reflection, deliberately separate from the clean design narrative of Chapter 4.

### 6.5 Conclusions

The research question can now be answered with evidence. Stranger-discovery social software with no central server is viable for the core loop: message delivery is approximately 84 percent on the conservative gate and approximately 93 percent on the measurement-backed gate, and battery cost is approximately 9 percent per day. It is bounded not by the distributed hash table, whose record findability is robust at scale, but by the physical supply of inbound-reachable serving nodes, a floor that, per Section 3.2, any decentralised social application requiring stranger discovery would share. It is weakest in real-time exchange between two mobile peers. On metadata privacy it improves markedly on the centralised baseline in content, graph, and persistence, while retaining a residual per-message routing-metadata floor. Every lever that improves quality, namely photo compression, higher replication, a quiet reachability-gated serving tier, and a trusted relay set, is about manufacturing reachable capacity rather than about the DHT.

## 7. Conclusions and Future Work

### 7.1 Introduction

This project set out to determine how much of a stranger-discovery social application can run with no central server on commodity mobile devices, using dating as the stress test, and to measure the cost. It delivered a working Android system and a calibrated evaluation that answers the question rather than asserting an answer.

### 7.2 Future Work

The open work falls into two categories. The first converts the evaluation's remaining assumptions into measurements: a primary user study to replace the literature-derived behavioural model; a real-device battery anchor; a multi-container real-NAT hole-punch harness to replace the calibrated NAT-pair rates; and a real-bytes throughput validation. The second strengthens the system itself, and every item lowers one of the floors identified in Chapter 6: deploying a trusted relay set to enable the wired real-time path; enabling and validating the GossipSub hardening (peer scoring, gating, and validators) to give the discovery mesh active flooding and eclipse resistance; implementing the iOS cryptography and transport actuals; adding forward secrecy and sealed-sender behind the existing messaging interface; separating the signing and key-agreement keys; enforcing blocking locally; and adding cache replication for popular records to spread serving load. A natural extension, suggested by the kit analysis of Section 3.2, is to instantiate the same substrate as a broadcast-style or local-community social application, to test whether the reachability floor generalises and whether the fan-out and storage burden that dating avoids reappears as the dominant constraint.

### 7.3 Conclusions

Aura demonstrates that the core of a stranger-discovery social application can run with no operator-run server, demonstrated on dating: identity, discovery, verified fetch, matching, and end-to-end encrypted messaging with offline delivery all run with no server, and the distributed hash table that many assume to be the fragile component is in fact robust at the scale tested. The genuine limit is not cryptographic or algorithmic but physical: consumer mobile networks supply too few inbound-reachable devices to guarantee real-time exchange, so a fully server-free design must accept best-effort, store-and-forward delivery, or introduce a small and deliberate reachable tier. The contribution of this project is therefore twofold: a coherent, working architecture for decentralised stranger-discovery social applications, demonstrated on the hardest representative case, and an honest, measurement-backed map of exactly where the no-central-server ideal meets the limits of the mobile network. The project set out to find that limit, and it found it.

## References

[1] Business of Apps. Dating App Revenue and Usage Statistics (2026). businessofapps.com.

[2] Mozilla Foundation. \*Privacy Not Included: A Buyer's Guide for Connected Products (2024).

[3] Ghorayshi, A. and Ray, S. Grindr Is Sharing The HIV Status Of Its Users With Other Companies. BuzzFeed News (2018).

[4] Krebs, B. Online Cheating Site AshleyMadison Hacked. Krebs on Security (2015).

[5] CBC News. Ashley Madison hack: 2 unconfirmed suicides linked to breach (2015).

[6] Lutz, C. and Ranzini, G. Where Dating Meets Data: Investigating Social and Institutional Privacy Concerns on Tinder. Social Media + Society (2017).

[7] Stardust, Z. et al. Surveillance does not equal safety: Police, data and consent on dating apps. Crime, Media, Culture (2023).

[8] Condie, J. et al. The Trouble with Tinder: The Ethical Complexities of Researching Location-Aware Social Discovery Apps (2017).

[9] Horan, T. Paying for Privacy? Evaluating Consumer Willingness to Pay for Data Ownership. SSRN (2025).

[10] Winegar, A. G. and Sunstein, C. R. How Much Is Data Privacy Worth? A Preliminary Investigation. Journal of Consumer Policy (2019).

[11] Mueller-Tribbensee, T. et al. Paying for Privacy: Pay-or-Tracking Walls. arXiv (2024).

[12] McCarthy, P. X. et al. Evolution of diversity and dominance of companies in online activity. PLOS ONE (2021).

[13] European Union. Art. 25 GDPR - Data protection by design and by default.

[14] Kleppmann, M. et al. Bluesky and the AT Protocol: Usable Decentralized Social Media. Proceedings of the ACM CoNEXT (2024).

[15] AT Protocol. Protocol Overview, Repository, and DID documentation. atproto.com.

[16] Briar Project. A Quick Overview of the Protocol Stack. Briar developer wiki (2019).

[17] Tarr, D. et al. Secure Scuttlebutt: An Identity-Centric Protocol for Subjective and Decentralized Applications. ACM ICN (2019).

[18] Kermarrec, A.-M. et al. Gossiping with Append-Only Logs in Secure-Scuttlebutt (2021).

[19] Orel, E. M. Stability Analysis of the Architecture of Messaging Systems with a Decentralized Topology. Automatic Control and Computer Sciences (2023).

[20] Weinberger, D. How the father of the World Wide Web plans to reclaim it (Solid). Digital Trends (2016).

[21] Leitao, J. et al. Epidemic Broadcast Trees. IEEE SRDS (2007).

[22] Maymounkov, P. and Mazieres, D. Kademlia: A Peer-to-Peer Information System Based on the XOR Metric (2002).

[23] Grinius, V. IPv6 Adoption: Where Are We? IPXO (2024).

[24] AnyConnect. STUN, TURN, and ICE NAT Traversal Protocols.

[25] libp2p. DCUtR hole-punching measurement campaign (~70% success over ~4.4M attempts). libp2p.io (2022).

[26] Tailscale / WebRTC. NAT traversal consensus on hard-NAT pairs.

[27] Signal Messenger. Technical documentation: X3DH and the Double Ratchet algorithm.

[28] hashberg-io. dag-cbor: a Python implementation of DAG-CBOR (DASL/IPLD).

[29] Kassinen, O. et al. Battery life of mobile peers with UMTS and WLAN in a Kademlia-based P2P overlay. IEEE PIMRC (2009).

[30] Huang, J. et al. A Close Examination of Performance and Power Characteristics of 4G LTE Networks. ACM MobiSys (2012).

[31] Ookla. Ireland Connectivity Report, H1 2025.

[32] nodejs-mobile. nodejs-mobile-react-native (2025).

[33] Android Developers. Android NDK documentation.

[34] go-libp2p v0.48.0, go-libp2p-kad-dht v0.40.0, go-libp2p-pubsub v0.16.0. Module documentation and source.

[35] Nostr. Notes and Other Stuff Transmitted by Relays: protocol specification (NIPs).

## Appendix A: Prompts Used with Gen AI

### C.1 Prompts related to Report Writing

Grammarly was used extensively throughout this report

### C.2 Prompts Related to Research

Claude was used for fact checking and additional source lookup.

### C.3 Prompts Related to Design

UI Design was designed with the help of Claude Code

### C.4 Prompts Related to Coding

Claude Code was used for Autoline completion, commenting, debugging and testing.
