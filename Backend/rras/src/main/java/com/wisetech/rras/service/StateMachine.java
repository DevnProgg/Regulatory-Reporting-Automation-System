package com.wisetech.rras.service;

import com.wisetech.rras.model.snapshot.Snapshot;
import com.wisetech.rras.model.snapshot.States;
import org.springframework.stereotype.Service;

@Service
public class StateMachine extends States {


    public Snapshot CreatedSnapshot(Snapshot snapshot){
        snapshot.setStatus(DRAFT);
        return snapshot;
    }

    public Snapshot ValidatedSnapshot(Snapshot snapshot){
        snapshot.setStatus(VALIDATED);
        return snapshot;
    }

    public  Snapshot CalculatedSnapshot (Snapshot snapshot){
        snapshot.setStatus(CALCULATED);
        return snapshot;
    }

    public Snapshot ApprovedSnapshot ( Snapshot snapshot){
        snapshot.setStatus(APPROVED);
        return snapshot;
    }
}