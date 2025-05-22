# GRANDPA (Block Finalization)

Fruzhin implements the GRANDPA (GHOST-based Recursive ANcestor Deriving Prefix Agreement) finality protocol, which is 
used in Polkadot and Substrate-based chains to provide deterministic finality to blocks.

## Overview

GRANDPA is a finality gadget that works alongside BABE to provide deterministic finality to blocks. It operates in 
rounds where validators vote on blocks they believe should be finalized, with finality achieved when a super-majority 
(2/3 + 1) of validators agree on a block.

## Key Components

### 1. GrandpaService
The main service that coordinates the GRANDPA consensus process:
- Manages round transitions and voting
- Handles justification processing
- Coordinates with other consensus components
- Manages authority set changes

### 2. GrandpaRound
Manages individual voting rounds:
- Tracks pre-votes and pre-commits
- Implements the GHOST algorithm for block selection
- Manages round state transitions
- Handles vote aggregation and threshold checking
- Coordinates finalization attempts

### 3. GrandpaSetState
Manages the validator set state:
- Tracks current and next authority sets
- Handles authority set changes (forced and scheduled)
- Maintains round state persistence
- Manages authority weights and thresholds

### 4. GrandpaMessageHandler
Handles GRANDPA network messages:
- Processes incoming votes and commits
- Verifies message signatures
- Manages catch-up requests
- Handles equivocation detection
- Coordinates message broadcasting

### 5. JustificationVerifier
Verifies block finality justifications:
- Validates vote signatures
- Checks vote weights and thresholds
- Ensures proper authority set participation
- Verifies block ancestry

## Voting Process

1. **Round Initiation**
    - New rounds start from the last finalized block
    - Primary voter is determined for each round
    - Round state is initialized with current authority set

2. **Pre-vote Phase**
    - Validators broadcast pre-votes for their preferred blocks
    - GHOST algorithm selects the best block based on votes
    - Threshold of 2/3 + 1 weight required to proceed

3. **Pre-commit Phase**
    - Validators pre-commit to blocks they believe will be finalized
    - Best final candidate is selected based on pre-commits
    - Finality is achieved when threshold is reached

4. **Finalization**
    - Block is finalized when sufficient pre-commits are collected
    - Justification is created and stored
    - Authority set changes are applied if scheduled

## Authority Set Management

- Authority sets can change through:
    - Scheduled changes (at specific block numbers)
    - Forced changes (immediate effect)
- Changes are tracked and applied when blocks are finalized
- Authority weights are maintained for vote counting
- Set ID is incremented with each authority set change

## Integration with Runtime

The implementation integrates with the runtime through various API calls:
- `getGrandpaApiAuthorities`: Retrieves current authority set
- `generateGrandpaKeyOwnershipProof`: Generates proofs for equivocation reporting
- `submitReportGrandpaEquivocationUnsignedExtrinsic`: Reports equivocation cases

For more detailed information about the GRANDPA consensus protocol and finality process, please refer to the official 
Polkadot specification at https://spec.polkadot.network/sect-finality.