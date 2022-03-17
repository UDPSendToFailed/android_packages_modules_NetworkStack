/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.networkstack.apishim.api29;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.android.networkstack.apishim.common.UnsupportedApiLevelException;
import com.android.networkstack.apishim.common.VpnManagerShim;

/**
 * Implementation of {@link VpnManagerShim} for API 29.
 */
@RequiresApi(Build.VERSION_CODES.Q)
public class VpnManagerShimImpl implements VpnManagerShim {
    protected VpnManagerShimImpl(Context context) {}

    /**
     * Get a new instance of {@link VpnManagerShim}.
     */
    public static VpnManagerShim newInstance(Context context) throws UnsupportedApiLevelException {
        return new VpnManagerShimImpl(context);
    }

    /**
     * See android.net.VpnManager#startProvisionedVpnProfileSession
     */
    @Override
    public String startProvisionedVpnProfileSession() throws UnsupportedApiLevelException {
        // Not supported for API 29.
        throw new UnsupportedApiLevelException("Not supported in API 29.");
    }
}
