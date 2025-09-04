package me.koyere.ecoxpert.modules.professions;

public enum ProfessionRole {
    SAVER,
    SPENDER,
    TRADER,
    INVESTOR,
    SPECULATOR,
    HOARDER,
    PHILANTHROPIST;

    public static ProfessionRole fromString(String s) {
        return ProfessionRole.valueOf(s.trim().toUpperCase().replace(' ', '_'));
    }
}

