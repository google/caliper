package dk.ilios.spanner.internal.trial;

import java.util.UUID;

import dk.ilios.spanner.internal.Experiment;

public final class TrialContext {

    private final UUID trialId;
    private final int trialNumber;
    private final Experiment experiment;

    public TrialContext(UUID trialId, int trialNumber, Experiment experiment) {
        this.trialId = trialId;
        this.trialNumber = trialNumber;
        this.experiment = experiment;
    }

    public UUID getTrialId() {
        return trialId;
    }

    public int getTrialNumber() {
        return trialNumber;
    }

    public Experiment getExperiment() {
        return experiment;
    }
}
