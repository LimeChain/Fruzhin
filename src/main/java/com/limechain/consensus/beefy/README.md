# BEEFY

BEEFY (Bridge Efficiency Enabling Finality Yielder) is a secondary protocol running alongside GRANDPA Finality to 
support efficient bridging with non-Substrate blockchains, particularly Ethereum mainnet. 

## Overview

BEEFY acts as a bridge-specific gadget that complements GRANDPA Finality. It leverages GRANDPA's assumptions and is 
built on top of it to provide bridging capabilities. The protocol is designed to be lightweight and optimized for 
restricted environments like Ethereum smart contracts.

## Key Components

### 1. BeefyService
The main service that coordinates the BEEFY consensus process:
- Manages voting rounds and finalization
- Handles justification processing
- Coordinates with GRANDPA finality
- Manages authority set changes
- Implements the main voting loop

### 2. BeefyState
Manages the state of BEEFY consensus:
- Tracks current and next authority sets
- Maintains session information
- Manages finalized block numbers
- Handles pending justifications
- Coordinates with the runtime for state updates

### 3. BeefySession
Represents an active BEEFY session:
- Manages authority sets and their changes
- Tracks mandatory blocks
- Handles vote collection and verification
- Implements threshold-based finality
- Detects and reports equivocation

### 4. BeefyMessageHandler
Handles BEEFY network messages:
- Processes incoming votes and justifications
- Verifies message signatures
- Manages message broadcasting
- Coordinates with the network layer

### 5. BeefyNotificationEngine
Manages BEEFY network communication:
- Handles peer connections and handshakes
- Manages message routing
- Coordinates message broadcasting
- Maintains connection state

## Voting Process

1. **Round Initiation**
    - New rounds start from the last finalized block
    - Target block is selected based on GRANDPA finality
    - Authority set is verified for the round

2. **Vote Collection**
    - Validators sign and broadcast votes
    - Votes are collected and verified
    - Equivocation is detected and reported
    - Threshold checking is performed

3. **Finalization**
    - Block is finalized when threshold is reached
    - Justification is created and stored
    - Authority set changes are applied if scheduled

## Authority Set Management

- BEEFY authority sets are synchronized with GRANDPA's authority sets:
   - Authority changes in GRANDPA automatically trigger corresponding changes in BEEFY
   - BEEFY validators are the same as GRANDPA validators (But different keys are used)
   - Authority set changes are processed when GRANDPA finalizes blocks containing the change
- Changes are tracked and applied when blocks are finalized by GRANDPA
- Authority weights are maintained for vote counting
- Set ID is incremented with each authority set change

## Integration with Runtime

The implementation integrates with the runtime through various API calls:
- `getBeefyGenesis`: Retrieves the BEEFY genesis block
- `getBeefyValidatorSet`: Gets the current validator set
- `generateBeefyKeyOwnershipProof`: Generates proofs for equivocation reporting
- `submitReportBeefyDoubleVotingUnsignedExtrinsic`: Reports equivocation cases

For more detailed information about the BEEFY consensus protocol and bridging process, please refer to the official 
Polkadot specification at https://spec.polkadot.network/sect-finality#sect-grandpa-beefy.