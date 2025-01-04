package net.slipcor.pvparena.compatibility;

import static net.slipcor.pvparena.core.VersionUtils.isApiVersionNewerThan;

public class Constants {
    public static int INFINITE_EFFECT_DURATION = isApiVersionNewerThan("1.19.0")  ? -1 : Integer.MAX_VALUE;
}
