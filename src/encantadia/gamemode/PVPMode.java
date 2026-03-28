package encantadia.gamemode;

import encantadia.story.StoryType;

public class PVPMode extends BaseModeScreen {

    private static final String BG_PATH = "/resources/background (2).png";

    public PVPMode() { init(); }

    @Override protected String       getBackgroundPath() { return BG_PATH; }
    @Override protected String       getWindowTitle()    { return "Encantadia: Echoes of the Gem — PVP Mode"; }
    @Override protected StoryType    getStoryType()      { return StoryType.PVP_LORE; }
    @Override protected GameModeType getGameModeType()   { return GameModeType.PVP; }
}