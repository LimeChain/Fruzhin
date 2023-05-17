package com.limechain.sync;

import lombok.Setter;

@Setter
public class ImportResult {
    boolean validVoter;
    boolean duplicated;
    boolean equivocation;

    public boolean isVoterValid(){
        return validVoter;
    };
    public boolean isDuplicated(){
        return duplicated;
    }
    public boolean isEquivocation(){
        return equivocation;
    }
}
