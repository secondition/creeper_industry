package com.secondition.creeperindustry.content.logistics.launcher;

public enum RocketLaunchResult {
    SUCCESS("message.creeper_industry.rocket_launcher.success"),
    INVALID_LAUNCHER("message.creeper_industry.rocket_launcher.invalid"),
    COOLDOWN("message.creeper_industry.rocket_launcher.cooldown"),
    MISSING_ROCKET("message.creeper_industry.rocket_launcher.missing_rocket"),
    MISSING_CARGO("message.creeper_industry.rocket_launcher.missing_cargo"),
    MISSING_ADDRESS("message.creeper_industry.rocket_launcher.missing_address"),
    WRONG_DIMENSION("message.creeper_industry.rocket_launcher.wrong_dimension"),
    INVALID_TARGET("message.creeper_industry.rocket_launcher.invalid_target"),
    TOO_FAR("message.creeper_industry.rocket_launcher.too_far"),
    FLIGHT_LIMIT("message.creeper_industry.rocket_launcher.flight_limit"),
    ENTITY_CREATION_FAILED("message.creeper_industry.rocket_launcher.entity_failed");

    private final String translationKey;

    RocketLaunchResult(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
