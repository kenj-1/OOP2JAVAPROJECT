package encantadia.gamemode;

import encantadia.story.StoryType;

public class ArcadeMode extends BaseModeScreen {

    private static final String BG_PATH = "/resources/background (2).png";

    public ArcadeMode() { init(); }

    @Override protected String       getBackgroundPath() { return BG_PATH; }
    @Override protected String       getWindowTitle()    { return "Encantadia: Echoes of the Gem — Arcade Mode"; }
    @Override protected StoryType    getStoryType()      { return StoryType.ARCADE_LORE; }
    @Override protected GameModeType getGameModeType()   { return GameModeType.ARCADE; }
}