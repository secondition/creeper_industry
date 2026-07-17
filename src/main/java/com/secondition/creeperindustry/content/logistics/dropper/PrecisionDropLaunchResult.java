package com.secondition.creeperindustry.content.logistics.dropper;

public enum PrecisionDropLaunchResult {
    SUCCESS("message.creeper_industry.precision_dropper.launched"),
    INVALID_MENU("message.creeper_industry.precision_dropper.invalid_menu"),
    INVALID_TARGET("message.creeper_industry.precision_dropper.invalid_target"),
    OUTSIDE_WORLD_BORDER("message.creeper_industry.precision_dropper.outside_world_border"),
    NOT_ON_PLATFORM("message.creeper_industry.precision_dropper.not_on_platform"),
    PLAYER_UNAVAILABLE("message.creeper_industry.precision_dropper.player_unavailable"),
    PLAYER_BUSY("message.creeper_industry.precision_dropper.player_busy"),
    DROPPER_BUSY("message.creeper_industry.precision_dropper.dropper_busy"),
    CHUNK_LOAD_FAILED("message.creeper_industry.precision_dropper.chunk_load_failed");

    private final String translationKey;

    PrecisionDropLaunchResult(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean successful() {
        return this == SUCCESS;
    }
}
