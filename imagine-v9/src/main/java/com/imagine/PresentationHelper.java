/*  This file is part of Imagine.

	Imagine is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Imagine is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Imagine.  If not, see <http://www.gnu.org/licenses/> */

package com.imagine;

import android.app.Activity;
import android.app.Presentation;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;

final class PresentationHelper extends Presentation
        implements DialogInterface.OnDismissListener, SurfaceHolder.Callback2 {
    private static final String logTag = "Presentation";

    private final ContentViewV16Base contentView;

    private native void onSurfaceCreated(long nativeUserData, Surface surface);

    private native void onSurfaceRedrawNeeded(long nativeUserData);

    private native void onSurfaceDestroyed(long nativeUserData);

    private native void onWindowDismiss(long nativeUserData);

    PresentationHelper(Activity context, Display display, long nativeUserData) {
        super(context, display);
        setOnDismissListener(this);
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            contentView = new ContentViewV24(context, nativeUserData);
        } else {
            contentView = new ContentViewV16(context, nativeUserData);
        }
        show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().takeSurface(this);
        setContentView(contentView);
    }

    // called by the native code if it deinits the window
    public void deinit() {
        contentView.resetNativeUserData();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (contentView.nativeUserData != 0) {
            onWindowDismiss(contentView.nativeUserData);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (contentView.nativeUserData != 0)
            onSurfaceCreated(contentView.nativeUserData, holder.getSurface());
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        if (contentView.nativeUserData != 0)
            onSurfaceRedrawNeeded(contentView.nativeUserData);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (contentView.nativeUserData != 0)
            onSurfaceDestroyed(contentView.nativeUserData);
    }
}
