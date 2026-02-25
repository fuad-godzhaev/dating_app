



Decentralised Social Network
Interim Report


TU856 
BSc in Computer Science


Fuad Godzhaev
D20124630

Bianca Schoen Phelan

School of Computer Science
Technological University, Dublin

12/01/2026
Abstract

In recent years, there has been a growing concern about online privacy and the data collected by various social networks, such as Facebook, X/Twitter, or Tinder. Consumers, advocacy groups, and researchers alike are concerned that vast amounts of data are being held by a few companies that provide services that are difficult to replace for many reasons. One of these factors is the abundance of alternatives from new players in the field, largely due to the high cost of entry into the social network industry and fewer investments in start-ups covering this area, primarily caused by growing regulatory restrictions, high costs of hardware infrastructure and higher risk of failure.

This project aims to address multiple, seemingly unrelated barriers that developers face when trying to create new platforms for people to socialise by combining modern technologies with original internet principles and ideas, providing developers with a more affordable and streamlined approach to creating new social networks and enhancing privacy standards for general users.

The development of a decentralised social network app serves as a proof-of-concept, demonstrating the feasibility of creating such software and highlighting the pros and cons of adopting this non-standard approach. The successful development of the app would provide an alternative approach for smaller developer teams to create privacy-focused, cost-effective, and consumer-oriented social platforms that can accommodate innovations and experiments, enabling them to compete with industry leaders.

Declaration


I hereby declare that the work described in this dissertation is, except where otherwise stated, entirely my own work and has not been submitted as an exercise for a degree at this or any other university.


Signed:

___Fuad Godzhaev_______
Fuad Godzhaev

12/01/2026




Acknowledgements

To be added…


Contents
1. Introduction	1
1.1 Project Background	1
1.2 Project Description	1
1.3 Project Aims and Objectives	1
1.4  Project Scope	1
1.5 Thesis Roadmap	1
2. Literature Review	2
2.1 Introduction	2
2.2 Alternative Existing Solutions	2
2.3 Technologies Researched	2
2.4 Other Research	2
2.5 Existing Final Year Projects	2
2.6 Conclusions	2
3. System Analysis	3
3.1 System Overview	3
3.2 Requirements Gathering	3
3.3 Requirements Analysis	3
3.X Other Section	3
3.X Other Section	3
3.X Initial System Specification	3
3.X Conclusions	3
4. System Design	4
4.1 Introduction	4
4.2 Software Methodology	4
4.3 Overview of System	4
4.4 Design System	4
4.X Other Section	4
4.X Conclusions	4
5. Testing and Evaluation	5
5.1 Introduction	5
5.2 Plan for Testing	5
5.3 Plan for Evaluation	5
5.4 Conclusions	5
6. System Prototype	6
6.1 Introduction	6
6.2 Prototype Development	6
6.3 Results	6
6.4 Evaluation	6
6.5 Conclusions	6
7. Issues and Future Work	7
7.1 Introduction	7
7.2 Issues and Risks	7
7.3 Plans and Future Work	7
7.3.1 Project Plan with GANTT Chart	7
References	8
A)	Appendix A: System Model and Analysis	A-1
B)	Appendix B: Design	B-1
C)	Appendix C: Prompts Used with ChatGPT	C-1
D)	Appendix D: Additional Code Samples	D-1
E)	Appendix E:	E-1

       1. Introduction

### 1.1 Project Background

Under the blanket term “social network” there are lots of distinct platforms, each gathering and handling different data about its users. For this project, I’ve settled on a dating app for several reasons: the vast amounts of sensitive personal information it gathers, the relatively small amount of storage required per user, and the distinct ways users find and interact with each other within the app.
In April 2024, the Mozilla Foundation's "Privacy Not Included" research team evaluated 25 popular dating apps and concluded that 22 of them (88%) warranted privacy warnings—the worst results since the organisation began its evaluations in 2021 [1]. The authors noted that dating apps had "gotten worse for privacy" and that these platforms "just can't get enough of your data". This finding does not represent an isolated incident but rather a systemic pattern within an industry that has grown to serve over 350 million users globally, valued at between $6 billion and $9 billion annually [2]. From Grindr sharing users' HIV status with third-party analytics firms without explicit consent in 2018 [3], to the catastrophic Ashley Madison breach that exposed 37 million users in 2015 with two confirmed suicides linked to the exposure [4] [5], to ongoing concerns about intimate behavioural data being sold to brokers potentially accessible by government agencies without warrants [6], the dating app industry has demonstrated a persistent lack of protection of the sensitive information entrusted to it.
One of the root causes of these privacy failures is an architectural decision driven by economic incentives. Modern dating platforms operate on centralised infrastructure where user data—including precise geolocation, sexual orientation, messaging content, swipe behaviour, and response patterns—flows through servers controlled by a single company [7]. This architectural choice creates a "honeypot": a concentrated repository of extraordinarily sensitive personal information that presents an irresistible target for malicious actors, an invaluable asset for advertising-driven business models, and an ongoing liability for users who have little visibility into how their intimate data is processed, shared, or monetised.
The broader social network landscape shows similar patterns. A small number of players in the field – including Meta, X/Twitter, ByteDance, and Match Group – control platforms that billions of people rely on for daily communication, yet users possess minimal control over the data they generate, despite growing regulatory constraints. The very same constraints that, on the other hand, raised the cost of entry into the social network industry, alongside infrastructure expenses for global-scale server deployments. These barriers prevent new entrants from challenging established companies [8], which has led to a consolidation, resulting in what might be described as a decade-long stagnation in innovation around privacy-focused alternatives, as the fundamental client-server architecture upon which these platforms were built remains unchanged from its origins.
However, significant progress has been made in the domain of peer-to-peer (P2P) and decentralised systems. Projects such as Briar [9], Secure Scuttlebutt [10], and the AT Protocol powering Bluesky [11] have demonstrated that alternative architectures are technically feasible with some trade-offs. Briar implements encrypted mesh messaging through Tor, Bluetooth, and Wi-Fi Direct, providing exceptional privacy; however, it struggles with battery consumption and requires both parties to be online simultaneously and to know each other in advance. Scuttlebutt pioneered gossip-based social networking with append-only logs and cryptographic identities; however, its network declined from over 10,000 users at its peak to approximately 200 active users by 2021 due to scalability limitations and storage growth that became unsustainable. Bluesky's AT Protocol has achieved mainstream traction, growing from 10 million to over 27 million users between September 2024 and January 2025; however, it has faced criticism for re-centralising around relay infrastructure, with 99.9% of users hosting their data on Bluesky-operated servers rather than self-hosting [12].
These existing solutions represent both the promising innovations and the challenges that exist in decentralised social networking. Dating presents unique requirements that differ from social networking: the need for discovery among strangers, near real-time messaging, verification mechanisms to ensure safety, privacy when users need it, and an expectation of quick profile loading.
At the same time, the technical landscape has evolved in ways to make mobile P2P architectures more viable. IPv6 adoption on mobile networks has reached 87% among major US carriers and 50-70% globally, which will eliminate Network Address Translation (NAT) barriers once it reaches 100% [13]. Modern NAT traversal techniques can help in the meantime, with STUN, TURN, and ICE protocols achieving connection success rates of over 95% when properly implemented [14]. The Signal Protocol has proven that end-to-end encryption can operate at a billion-user scale without compromising user experience [15]. Perhaps most significantly, the average smartphone now has computational capabilities that exceed the servers that powered early internet infrastructure, thus potentially allowing users’ devices to become their own Personal Data Servers (PDS), eliminating the need for centralised infrastructure entirely.
In summary, users' growing demand for privacy, the technical evolution of P2P protocols and broad dissatisfaction with existing dating platforms create a window of opportunity to explore whether a fundamentally different architecture is achievable and viable. The question this project aims to answer is whether decentralised, privacy-preserving dating apps are technically feasible, which could represent a template for a new type of social software, returning control to users and lowering barriers for developers who wish to create community-focused platforms without the ever-growing capital requirements.

### 1.2 Project Description

This project develops a proof-of-concept Android app demonstrating that a fully functional dating platform can operate without centralised server infrastructure. The core technology treats each user’s smartphone as a PDS: a device that stores its owner’s profile, handles cryptographic identity, participates in distributed discovery, and works with other devices to relay messages for offline users. Rather than uploading personal information to a third-party server, users retain ownership of their data while still enabling the discovery based on matching algorithms expected from modern dating apps.
The app uses multiple existing technologies to address challenges of discovery, asynchronous message delivery, and NAT restrictions:
Discovery: When users are physically near one another – at social venues, events or public spaces – the app advertises an encrypted, changing identifier via Bluetooth Low Energy (BLE). Nearby devices running the app can discover one another without internet connectivity, exchange encrypted profile information directly over Bluetooth, and potentially match based on mutual interest. This enables more organic, real-world discovery while preserving privacy through cryptographic anonymisation. For discovering potential matches across distances, the app will participate in a Kademlia-based distributed hash table using the libp2p networking stack. Users periodically send “heartbeat” signals to the DHT containing approximate locations (geohash-encoded to a few-km precision), age ranges, and interest categories – enough for discovery without exposing precise personal details. Other users query the DHT for profiles matching their preferences, retrieve profile data either directly from the owner’s device or from cached copies held by other network participants, and initiate peer-to-peer connections for real-time communication.
Offline Messaging: Dating apps must support asynchronous communication. The relay layer can address this through two different methods: a mandatory cooperative participation where each device agrees to temporarily store some encrypted messages destined for offline users, or a low-power stand-by mode of a sender’s server, which will wait for the recipient's heartbeat and attempt to deliver the message again. All messages will be end-to-end encrypted (E2EE) to preserve confidentiality while enabling the store-and-forward functionality.
NAT Restrictions: One of the IPv6 design goals is to restore end-to-end network connectivity. Due to slow adoption of this new standard, we can not yet expect every user to have a unique IPv6 address, but this project will research and experiment with the new protocol, and will also implement NAT traversal techniques such as ICE, Session Traversal Utilities for NAT (STUN), and Traversal Using Relays around NAT (TURN).
Identity within the system will be self-sovereign, implemented using Decentralised Identifiers (DIDs) following the did:key specification. Upon first launch, the app generates an Ed25519 cryptographic key pair, derives a DID from the public key, and stores the private key in the device's hardware-backed secure enclave. This identity requires no registration with any authority—users cryptographically prove ownership of their identity through digital signatures, and can recover their identity using a seed phrase if they change devices. 
The project focuses on demonstrating technical feasibility rather than producing a commercially deployable app. The scope is limited to Android devices, dating functionality only (excluding friendship and networking modes), simple preference-based filtering (excluding machine learning-based matching algorithms), and text messaging (excluding video calling). These constraints enable me, as a single developer, to deliver a functional proof of concept within an academic year while still validating the core architectural idea.

### 1.3 Project Aims and Objectives

Overall aim:  Research the technical and practical feasibility of decentralised peer-to-peer social networking on mobile devices.
Objectives:
    • Adopt an on-device PDS using ATProtocol’s implementation
    • Implement proximity and long-distance discovery
    • Establish direct P2P connections with >90% success rate
    • Research into how P2P will work with the implementation of IPv6
    • Implement E2EE for messaging
    • Develop an offline message delivery mechanism, i.e. a distributed relay protocol
    • Create a working prototype demonstrating core P2P functionality
    • Evaluate the system with a small group of volunteer users

### 1.4  Project Scope

The project’s scope is defined by its focus on enhancing privacy, users’ control over data, and the ease of development of community-focused social apps.
In Scope:
    • Android-only development
    • Basic dating functionality with simple preference filtering
    • Community-focused, self-sustainable and scalable software
    • Discovery within a distributed system
    • P2P messaging
    • Relay Network
Out Of Scope:
    • iOS or Desktop adaptation
    • Complex matching algorithms
    • Content moderation
    • Systems for profit or payment systems

        1.4 Thesis Roadmap
To be added...
            1.4.1 Literature Review
            1.4.2 System Analysis
            1.4.3 System Design
            1.4.4 Testing and Evaluation
            1.4.5 System Prototype
            1.4.6 Issues and Future Work

    2. Literature Review

### 2.1 Introduction

In this chapter I examine existing research and solutions relevant to building decentralised P2P platforms. The review covers four main areas of the project: alternative decentralised social networking solutions that have attempted similar architectural approaches; the technologies required for implementation including P2P networking, cryptographic protocols and mobile networking constraints; domain-specific research on dating apps requirements and privacy concerns; and existing final years project that have researched related themes. 
The goal is to find out what these works have achieved, what seemed to cause troubles, what mistakes to avoid, and to justify technological and design choices for this project.

### 2.2 Alternative Existing Solutions

Structured in the order of relevance to this project:

### 2.2.1 Pinecone - Matrix P2P

Pinecone is an experimental fork of the decentralised messaging platform Matrix, which is known for it’s strong security and privacy features. The idea behind the experiment was to embed the distributed node functionality in the client app, thus allowing the users’ device to act as a host and a relay, allowing for true P2P and distributed functionality without the need for third-party relays. As per the developer’s note “Pinecone peering connections look like regular TCP or WebSocket connections and will work fine through firewalls or NATs”. This tunneling technique might become quite useful in case other tecniques fail. 
[TODO: Visual graph showcasing the architecture]
Key Technical Features:
    • End-to-end encryption via TLS for all Pinecone traffic 
    • μTP (Micro Transport Protocol) combined with TLS for congestion control 
    • Node mobility handling—SNEK routing responds well to topology changes, critical for mobile devices changing networks 
    • Cross-platform implementation in Go with successful deployment on macOS, Linux, Windows, Android, and iOS []
Limitations:
    • Pinecone explicitly does not provide anonymity—packets route directly (unlike Tor) and contain source/destination information in headers, though future versions may seal source addresses [] 
    • As of December 2023, P2P Matrix and Pinecone development was paused accoridng to Matrix.org Foundation’s update []. This highlites the resource challenges present in advancing experimental P2P protocols without budgets.

Relevance to This Project: Embedded server-in-client architecture; Multi-transport support; Node mobility handling; Hybrid topology approach; 
[TODO: Expand this section]

### 2.2.2 Briar Project

Briar is a contact-based peer-to-peer messenger designed for users requiring strong privacy guarantees against adversaries capable of detailed traffic monitoring [[16]]. The architecture implements three transport mechanisms for different use-cases: Tor hidden services for anonymous internet connectivity, Bluetooth for direct short-range communication and WiFi Direct for local high-bandwith transfers.
Key Technical Features:
    • Bramble Transport Protocol (BTP) provides confidentiality, integrity, authenticity, and forward secrecy across all transports 
    • Contact addition requires QR code exchange (BQP) or link sharing with 48-hour connection windows (BRP) 
    • No store-and-forward capability without the separate Briar Mailbox application [] 

Limitations:
    • Battery consumption identified as "the single most important issue" preventing regular usage []
    • Contact-only model prevents discovery among strangers—fundamentally incompatible with dating use cases 
    • Android-only deployment; iOS restrictions on background networking prevent porting 
    • Requires both parties online simultaneously without Mailbox 
Relevance to This Project: Briar demonstrates that secure P2P mobile communication is achievable but highlights the critical importance of battery optimisation and the need for asynchronous messaging mechanisms. The contact-only limitation confirms that dating applications require fundamentally different discovery architecture.

### 2.2.3 Bluesky (AT Protocol)

The AT Protocol, developed by Bluesky Social PBC, implements a relay-based federated architecture that has achieved significant mainstream adoption—growing from 10 million users in September 2024 to over 27 million by January 2025 [][].
Architecture Components:
    • Personal Data Servers (PDS): Host user repositories; low resource requirements enable self-hosting on Raspberry Pi 
    • Relays: Crawl PDSes aggregating updates into "firehose" stream; currently the most centralised component 
    • App Views: Consume firehose, maintain indices and timelines 
    • Lexicon: Schema system unifying APIs and data structures [] 

Technical Foundation:
    • Signed Merkle Search Trees storing data in DAG-CBOR encoding 
    • Decentralised Identifiers (did:plc or did:web) [] 
    • DNS domains as user handles (e.g., @nytimes.com) 
    • Cryptographic authentication of all repositories [] 
Centralisation Tensions:
    • Despite decentralisation goals, 99.9% of users (38.6M of 38.7M) host data on Bluesky-operated PDS [] 
    • Relay infrastructure costs ~$153/month for 6M users, creating economic barriers to competing relays 
    • PLC directory currently operated solely by Bluesky Social PBC 
    • Wikipedia notes relays "have been criticised as being the most centralised component" [] 
Relevance to This Project: AT Protocol demonstrates that "usable decentralised social media" can achieve mainstream adoption through a "no steps backwards" design philosophy—maintaining centralised platform convenience while enabling decentralisation []. The project adopts this hybrid philosophy but pushes further toward true P2P by eliminating relay dependency for messaging while potentially accepting some centralisation for discovery optimisation.


### 2.2.4 Mastodon (ActivityPub)

Mastodon implements the W3C ActivityPub protocol (January 2018) through server-to-server federation [].
Key Limitations:
    • UX complexity: Users must choose servers at signup affecting usernames 
    • Incomplete reply threads across server boundaries 
    • Migration changes usernames and requires old server cooperation 
    • Viral posts overwhelm originating servers 
    • Monthly active users peaked at 2.5M (November 2022), currently ~2.5M after significant churn [] 
Relevance to This Project: ActivityPub's federation complexity demonstrates that technical decentralisation alone is insufficient—user experience must remain simple to achieve adoption. The project prioritises hiding technical complexity behind familiar dating app interfaces.

### 2.2.5 Solid

Hello template text for Solid (Sir Tim Berners Lee)


### 2.2.6 Secure Scuttlebutt

Scuttlebutt implements an offline-first, gossip-based social protocol where each user maintains an append-only log of cryptographically signed messages []. The network topology mirrors the social graph—data replicates along trust relationships using the Epidemic Broadcast Tree protocol [].
Technical Architecture:
    • Identity: Ed25519 key pairs with public key serving as immutable identifier 
    • Feeds: Append-only message sequences containing author, sequence number, previous hash, timestamp, content, and signature [] 
    • Secret Handshake: Four-step mutual authentication providing forward secrecy and MITM resistance 
    • Box Stream: Encrypted communication with rotating nonces preventing replay attacks 
Scalability Analysis:
    • Network peaked at >10,000 users in 2019, declined to ~200 active users by 2021 [] 
    • Storage grows unbounded—following 100 active users producing 10 messages/day generates ~700MB yearly 
    • New user onboarding historically required downloading entire feed histories (potentially gigabytes) 
    • Gossip replication causes duplicate message transmission, unsuitable for metered mobile connections 
Pub Server Centralisation:
    • Publicly accessible SSB peers act as rendezvous points and relays 
    • Users join via invite codes, creating dependence on pub operators 
    • Introduces trust requirements and single points of failure 
Relevance to This Project: SSB's cryptographic identity model and append-only logs provide valuable architectural patterns. However, the unbounded storage growth and gossip inefficiency demonstrate why pure gossip protocols cannot scale to dating application requirements. The project will adopt SSB's identity principles while implementing bounded retention policies and more efficient synchronisation.


### 2.3 Technologies Researched

ATProto/PDS
ActivityPub
TCP/IP
UDP/PGM
IP Multicast
IPV4 / IPV6
Android/Android SDK
iOS
DHT/Kademlia
NAT/CGNAT/Traversal
BLE discovery
NPM
GitHub Actions
Terminux



### 2.4 Other Research

I2P

2.5 Existing Final Year Projects

This paper goes through the research and implementation of a social media which implements a new layer of security that verifies users using a Know Your Customer (KYC) process, where users use a legal document such as an ID, passport, or driver’s licence and a picture of themselves to verify that they are who they say they are.

The complexity lies in building a microservice architecture that follows and applies all the best practices. Services will be implemented as Application Programming Interface (API) and containerised using Docker and Docker Compose. Docker will greatly help isolate each service and create a network between containers/services. Handling the identification of a large number of users while ensuring that the system identifies them correctly without compromising the privacy and security of their data is a core complexity point of this project.

What technical architecture was used? A microservice architecture was employed, where the application is divided into smaller pieces, with each part or service responsible for specific functionality. More specifically, Docker was used to create microservices in containers, thereby avoiding dependency issues and making the application robust, scalable, and easier to develop.

While the idea of adding a mandatory KYC process does help resolving issues like bad actors creating bot networks to gather user’s personal data, possible impersonations and a wide spread of hateful speech and online bullying, it does not solve these issues completely, because no such systems are 100% correct, especially nowadays with a widespread of AI and the ease it bring in creating “deepfakes”, but it is definitely a step in the right direction, and making KYC mandatory works well with an idea of a dating app, where everyone is supposed to, and expects, to see real humans sharing their real photos, names and information. On the other hand, I did not see sufficient precautions being taken to prevent users’ personal data from being used maliciously after the identification process was completed, which creates potential security risks and may undermine users’ trust.


### 2.6 Conclusions

To be added….

    3. System Analysis

3.1 System Overview  
The goal is to make the app as simple as possible from user’s perspective. One of the main reasons why audiences abandon decentralised platforms is complexity in first-time set-up and usage. From a user perspective, the app will appear as a standard dating app: users create profiles with photos and descriptions of themselves, then filter their matches based on interests and location, swipe left or right to indicate interest and exchange messages with mutual matches. The decentralised architecture must remain invisible to the user, with exception of them getting a message at the first-launch, asking for their consent on using a small percentage of their device’s compute power with explanation as to why the app needs it.
User journey:
    1) Onboarding: User installs the app, creates profile.
    2) Discovery: App displays nearby profiles first, retrieved via BLE, or long-distance profiles retrieved via DHT; profiles load from owner’s device or cached copies.
    3) Matching: User swipes right or left to display their interest; mutual right-swipes create a match.
    4) Messaging: Matched users send encrypted messages in a P2P fashion; if recipient is offline, messages queue on sender’s or relay notes until delivery.
    5) Privacy Control: User can delete their profile, thus removing it from their own device and the DHT; cached copies expire over time.
[TODO: Use-case diagram]
 
### 3.2 Requirements Gathering

### 3.2.1 Stakeholder Analysis
Identify and consider key stakeholders
Describe how to collect requirements
Collect initial requirements

### 3.3 Requirements Analysis
Develop an initial systems model through analysis of requirements

### 3.4 Initial System Specification

Functional Requirements:
    • System must generate DID identity without external authority
    • System must discover users via BLE within approximately 30m range
    • System must discover users over the internet via DHT based on geohash location
    • System must encypt all messages
    • System must deliver messages to offline recipients
    • System must store encrypted copies of profiles on user devices
    • System must support profile photo storage and retrieval
    • System must impement swipe-based matching interface
    • System must support profile deletion with network propagation
    • System could display online/offline status of matches
    • System could display approximate distance of a profile from a user

Non-Functional Requirements:
    • Battery consumption (background): Target is <0.5% per minute
    • Message delivery latency (online): Target is <2 seconds
    • Message delivery latency (offline): Target is <5 minutes after recipient is online
    • P2P connection success rate: Target is >90%
    • Profile load time: Target is <3 seconds
    • DHT query response time: Target is <5 seconds
Identify an appropriate architecture for the system

### 3.5 Conclusions
To be added...

    4. System Design 

### 4.1 Introduction

Brief description of approach to turn requirements or specification into design

### 4.2 Software Methodology

Identify an appropriate methodology to develop the system 

### 4.3 Overview of System 

Describe the logical Architecture and initial physical infrastructure
Consider and briefly the system deployment or production environment

### 4.4 Design System 

Use design methodology to create design

### 4.X Other Section


### 4.X Conclusions




    5. Testing and Evaluation

### 5.1 Introduction

Describe testing and evaluation approaches appropriate to the parts of the system (Logical Architecture)

### 5.2 Plan for Testing

Identify the approach for testing each part of the system
Provide test plan in structured manner

### 5.3 Plan for Evaluation
Identify evaluation methodology for each part of the system

### 5.4 Conclusions



    6. System Prototype 

As least 2 pages, but as many as you like (with lots of code samples).

### 6.1 Introduction

Outline of approach to developing the prototype
Identify frameworks used for any part of the system

### 6.2 Prototype Development

Develop Code for each part of system (Logical Architecture)


### 6.3 Results

Perform tests identified in section 5

### 6.4 Evaluation 

Evaluate results 
Use evaluation approaches from Section 5 to assess the outcomes of the development

### 6.5 Conclusions


    7. Issues and Future Work

### 7.1 Introduction
Purpose of the section

#### 7.2 Issues and Risks
Describe aspects of the project that had different outcomes to the expected outcomes, based on evaluation of results
Identify key elements of uncertainty that will lead to risks in completing the project

### 7.3 Plans and Future Work
##Consider and briefly describe how to address any risks and how to complete the system 
##Consider and describe how to complete the system based on the available time
## PGM - Pragmatic General Multicast; Minimal UI/FRONT END work; 

### 7.3.1 Project Plan with GANTT Chart
Based on the future plans, describe how to complete the project
Break down the plan into a detailed yet realistic schedule.
Present a Gantt chart to summarise the project plan

    References

    A) [1]	‘*Privacy Not Included: A Buyer’s Guide for Connected Products’, Mozilla Foundation. Accessed: Jan. 12, 2026. [Online]. Available: https://www.mozillafoundation.org/en/privacynotincluded/articles/data-hungry-dating-apps-are-worse-than-ever-for-your-privacy/
    B) [2]	‘Dating App Revenue and Usage Statistics (2026)’, Business of Apps. Accessed: Jan. 14, 2026. [Online]. Available: https://www.businessofapps.com/data/dating-app-market/
    C) [3]	A. G. Ray Sri, ‘Grindr Is Sharing The HIV Status Of Its Users With Other Companies’, BuzzFeed News. Accessed: Jan. 14, 2026. [Online]. Available: https://www.buzzfeednews.com/article/azeenghorayshi/grindr-hiv-status-privacy
    D) [4]	‘Online Cheating Site AshleyMadison Hacked – Krebs on Security’. Accessed: Jan. 14, 2026. [Online]. Available: https://krebsonsecurity.com/2015/07/online-cheating-site-ashleymadison-hacked/
    E) [5]	CBC, ‘Ashley Madison hack: 2 unconfirmed suicides linked to breach, Toronto police say’, CBC News, Aug. 24, 2015. Accessed: Jan. 21, 2026. [Online]. Available: https://www.cbc.ca/news/canada/toronto/ashley-madison-hack-2-unconfirmed-suicides-linked-to-breach-toronto-police-say-1.3201432
    F) [6]	‘This cuffing season, it’s time to consider the privacy of dating apps’, Brookings. Accessed: Jan. 14, 2026. [Online]. Available: https://www.brookings.edu/articles/this-cuffing-season-its-time-to-consider-the-privacy-of-dating-apps/
    G) [7]	J. srivastava, ‘Dating Application System Design’, System Design Concepts. Accessed: Jan. 14, 2026. [Online]. Available: https://medium.com/system-design-concepts/dating-application-system-design-aae411412267
    H) [8]	M. Kleppmann et al., ‘Bluesky and the AT Protocol: Usable Decentralized Social Media’, in Proceedings of the ACM Conext-2024 Workshop on the Decentralization of the Internet, Dec. 2024, pp. 1–7. doi: 10.1145/3694809.3700740.
    I) [9]	‘How it works - Briar’. Accessed: Jan. 21, 2026. [Online]. Available: https://briarproject.org/how-it-works/
    J) [10]	D. Tarr, E. Lavoie, A. Meyer, and C. Tschudin, ‘Secure Scuttlebutt: An Identity-Centric Protocol for Subjective and Decentralized Applications’, in Proceedings of the 6th ACM Conference on Information-Centric Networking, in ICN ’19. New York, NY, USA: Association for Computing Machinery, Sep. 2019, pp. 1–11. doi: 10.1145/3357150.3357396.
    K) [11]	‘Protocol Overview - AT Protocol’. Accessed: Jan. 21, 2026. [Online]. Available: https://atproto.com/guides/overview
    L) [12]	‘ATP/bsky Dominance Index’. Accessed: Oct. 29, 2025. [Online]. Available: https://burningtree.github.io/bsky-dominance/
    M) [13]	V. Grinius, ‘IPv6 Adoption: Where Are We? 2024 Updated’, IPXO. Accessed: Jan. 14, 2026. [Online]. Available: https://www.ipxo.com/blog/ipv6-adoption/
    N) [14]	‘STUN, TURN, and ICE NAT Traversal Protocols’, AnyConnect. Accessed: Jan. 14, 2026. [Online]. Available: https://anyconnect.com/stun-turn-ice/
    O) [15]	‘Documentation’, Signal Messenger. Accessed: Jan. 21, 2026. [Online]. Available: https://signal.org/docs/
    P) [16]	‘A Quick Overview of the Protocol Stack · Wiki · briar / briar · GitLab’, GitLab. Accessed: Oct. 27, 2025. [Online]. Available: https://code.briarproject.org/briar/briar/-/wikis/A-Quick-Overview-of-the-Protocol-Stackstem Model and Analysis

Details of Requirements gathering and analysis method

To be added...
    Q) Appendix B: Design 

Details of design approach used
System elements, components, classes

To be added….
    R) Appendix C: Prompts Used with ChatGPT
Grammarly was used to improve the grammar, clarity, tone and delivery of the report.

    S) Appendix D: Additional Code Samples

Provide additional code samples 
Organised by Logical Architecture

To be added...
