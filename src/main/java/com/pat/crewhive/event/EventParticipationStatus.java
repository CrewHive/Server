package com.pat.crewhive.event;

/**
 * Stato di un partecipante rispetto a un evento. Il creatore è sempre {@link #ACCEPTED};
 * un invitato a un evento PRIVATE parte da {@link #PENDING} finché non risponde.
 */
public enum EventParticipationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
