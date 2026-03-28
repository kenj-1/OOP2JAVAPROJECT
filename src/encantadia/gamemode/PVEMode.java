package encantadia.gamemode;

import encantadia.story.StoryType;

public class PVEMode extends BaseModeScreen {

    private static final String BG_PATH = "/resources/background (2).png";

    public PVEMode() { init(); }

    @Override protected String       getBackgroundPath() { return BG_PATH; }
    @Override protected String       getWindowTitle()    { return "Encantadia: Echoes of the Gem — PVE Mode"; }
    @Override protected StoryType    getStoryType()      { return StoryType.PVE_LORE; }
    @Override protected GameModeType getGameModeType()   { return GameModeType.PVE; }
}