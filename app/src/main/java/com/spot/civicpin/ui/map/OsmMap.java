package com.spot.civicpin.ui.map;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.spot.civicpin.R;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.File;

/**
 * OpenStreetMap setup shared by every map in the app. No API key or Google account needed.
 * Tiles: standard OpenStreetMap tiles (attribution shown on the map).
 */
public final class OsmMap {

    private static boolean configured;

    private OsmMap() {}

    /** Must run before inflating any MapView. */
    public static void init(Context context) {
        if (configured) return;
        Context app = context.getApplicationContext();
        org.osmdroid.config.IConfigurationProvider conf = org.osmdroid.config.Configuration.getInstance();
        conf.setUserAgentValue(app.getPackageName()); // tile servers require an identifying user agent
        File base = new File(app.getCacheDir(), "osmdroid");
        conf.setOsmdroidBasePath(base);
        conf.setOsmdroidTileCache(new File(base, "tiles")); // app cache dir: no storage permission needed
        configured = true;
    }

    public static void setup(MapView map) {
        map.setTileSource(TileSourceFactory.MAPNIK); // standard OpenStreetMap tiles, no key
        map.setTilesScaledToDpi(true);
        boolean dark = (map.getContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (dark) map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMinZoomLevel(4.0);
        map.setMaxZoomLevel(19.0);
        map.setVerticalMapRepetitionEnabled(false);
        CopyrightOverlay copyright = new CopyrightOverlay(map.getContext());
        copyright.setTextSize(9);
        map.getOverlays().add(copyright);
    }

    public static int colorFor(Context context, String category) {
        int res = R.color.cat_others;
        if ("Garbage".equalsIgnoreCase(category)) res = R.color.cat_garbage;
        else if ("Water_Leak_Sewer".equalsIgnoreCase(category)) res = R.color.cat_water;
        else if ("Potholes".equalsIgnoreCase(category)) res = R.color.cat_potholes;
        return ContextCompat.getColor(context, res);
    }

    /** Teardrop pin with a white ring and soft shadow, drawn in code. */
    public static Drawable pin(Context context, int color) {
        float d = context.getResources().getDisplayMetrics().density;
        int w = Math.round(34 * d), h = Math.round(44 * d);
        float r = 13 * d, cx = w / 2f, cy = r + 3 * d;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Path drop = new Path();
        drop.addCircle(cx, cy, r, Path.Direction.CW);
        Path tip = new Path();
        tip.moveTo(cx - r * 0.62f, cy + r * 0.78f);
        tip.lineTo(cx, h - 2 * d);
        tip.lineTo(cx + r * 0.62f, cy + r * 0.78f);
        tip.close();
        drop.op(tip, Path.Op.UNION);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        fill.setShadowLayer(3 * d, 0, 1.5f * d, 0x40000000);
        c.drawPath(drop, fill);

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2.5f * d);
        ring.setColor(0xFFFFFFFF);
        c.drawPath(drop, ring);

        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, r * 0.36f, dot);
        return new BitmapDrawable(context.getResources(), bmp);
    }
}
