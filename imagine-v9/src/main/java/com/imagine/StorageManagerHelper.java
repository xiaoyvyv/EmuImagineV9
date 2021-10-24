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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import androidx.annotation.RequiresApi;

import java.util.List;

final class StorageManagerHelper {
    private static final String logTag = "StorageManagerHelper";

    private static native void volumeEnumerated(long nativeUserData, String path, String name);

    private static int pathPosition = -1;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void enumVolumes(Activity act, long nativeUserData) {
        if (pathPosition == -2) {
            return;
        }
        final StorageManager sm = (StorageManager) act.getSystemService(Context.STORAGE_SERVICE);
        final List<StorageVolume> volumes = sm.getStorageVolumes();
        for (final StorageVolume vol : volumes) {
            if (vol.isPrimary())
                continue;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                final String path = vol.getDirectory().getAbsolutePath();
                if (path.indexOf('/') != 0)
                    continue;
                volumeEnumerated(nativeUserData, vol.getDescription(act), path);
            } else {
                Parcel p = Parcel.obtain();
                vol.writeToParcel(p, 0);
                String path = null;
                if (pathPosition < 0) {
                    p.setDataPosition(0);
                    while (p.dataPosition() < p.dataSize()) {
                        int pos = p.dataPosition();
                        final String s = p.readString();
                        if (s != null && s.startsWith("/storage")) {
                            //Log.v(logTag, "found path string:" + s + " @ position:" + pos + ")");
                            pathPosition = pos;
                            path = s;
                            break;
                        }
                    }
                    if (pathPosition < 0) {
                        pathPosition = -2;
                        p.recycle();
                        return;
                    }
                } else {
                    p.setDataPosition(pathPosition);
                    path = p.readString();
                }
                p.recycle();
                if (path == null || path.indexOf('/') != 0)
                    continue;
                volumeEnumerated(nativeUserData, vol.getDescription(act), path);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static StorageVolume findVolume(Activity act, String name) {
        final StorageManager sm = (StorageManager) act.getSystemService(Context.STORAGE_SERVICE);
        if (name.equalsIgnoreCase("primary"))
            return sm.getPrimaryStorageVolume();
        for (final StorageVolume vol : sm.getStorageVolumes()) {
            final String uuid = vol.getUuid();
            if (uuid != null && uuid.equalsIgnoreCase(name))
                return vol;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static String pathFromOpenDocumentTreeResult(Activity act, Intent intent) {
        final Uri uri = intent.getData();
        final List<String> pathSegment = uri.getPathSegments();
        final String[] split = pathSegment.get(1).split(":");
        final StorageVolume vol = findVolume(act, split[0]);
        if (vol == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return vol.getDirectory().getAbsolutePath() + "/" + split[1];
        }
        return null;
    }
}
