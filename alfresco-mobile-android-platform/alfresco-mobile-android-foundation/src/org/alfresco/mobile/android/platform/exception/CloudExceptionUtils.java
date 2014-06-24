/*******************************************************************************
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco Mobile for Android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.alfresco.mobile.android.platform.exception;

import org.alfresco.mobile.android.api.exceptions.AlfrescoServiceException;
import org.alfresco.mobile.android.api.exceptions.AlfrescoSessionException;
import org.alfresco.mobile.android.api.exceptions.ErrorCodeRegistry;
import org.alfresco.mobile.android.async.session.LoadSessionCallBack.LoadAccountErrorEvent;
import org.alfresco.mobile.android.foundation.R;
import org.alfresco.mobile.android.platform.EventBusManager;
import org.alfresco.mobile.android.ui.fragments.SimpleAlertDialogFragment;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.http.HttpStatus;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public final class CloudExceptionUtils
{

    private static final String TAG = CloudExceptionUtils.class.getName();

    private CloudExceptionUtils()
    {
    };

    public static void handleCloudException(Context context, Long accountId, Exception exception, boolean forceRefresh)
    {
        Log.w(TAG, Log.getStackTraceString(exception));
        if (exception instanceof AlfrescoSessionException)
        {
            AlfrescoSessionException ex = ((AlfrescoSessionException) exception);
            switch (ex.getErrorCode())
            {
                case ErrorCodeRegistry.SESSION_API_KEYS_INVALID:
                case ErrorCodeRegistry.SESSION_REFRESH_TOKEN_EXPIRED:
                    manageException(context, forceRefresh);
                    return;
                default:
                    if (ex.getMessage().contains("No authentication challenges found") || ex.getErrorCode() == 100)
                    {
                        manageException(context, forceRefresh);
                        return;
                    }
                    break;
            }
        }

        if (exception instanceof AlfrescoServiceException)
        {
            AlfrescoServiceException ex = ((AlfrescoServiceException) exception);
            if ((ex.getErrorCode() == 104 || (ex.getMessage() != null && ex.getMessage().contains(
                    "No authentication challenges found"))))
            {
                manageException(context, forceRefresh);
                return;
            }
            else
            {
                Bundle b = new Bundle();
                b.putInt(SimpleAlertDialogFragment.ARGUMENT_ICON, R.drawable.ic_alfresco_logo);
                b.putInt(SimpleAlertDialogFragment.ARGUMENT_TITLE, R.string.error_general_title);
                b.putInt(SimpleAlertDialogFragment.ARGUMENT_POSITIVE_BUTTON, android.R.string.ok);
                b.putInt(SimpleAlertDialogFragment.ARGUMENT_MESSAGE,
                        SessionExceptionHelper.getMessageId(context, exception));
                // ActionManager.actionDisplayDialog(context, b);
                return;
            }
        }

        if (exception instanceof CmisConnectionException)
        {
            CmisConnectionException ex = ((CmisConnectionException) exception);
            if (ex.getMessage().contains("No authentication challenges found"))
            {
                manageException(context, forceRefresh);
                return;
            }
        }

        if (exception instanceof AlfrescoSessionException)
        {
            int messageId = R.string.error_session_notfound;
            AlfrescoSessionException se = ((AlfrescoSessionException) exception);
            if (se.getErrorCode() == ErrorCodeRegistry.GENERAL_HTTP_RESP && se.getMessage() != null
                    && se.getMessage().contains(HttpStatus.SC_SERVICE_UNAVAILABLE + ""))
            {
                messageId = R.string.error_session_cloud_unavailable;
            }

            EventBusManager.getInstance().post(new LoadAccountErrorEvent(null, accountId, exception, messageId));
            return;
        }
    }

    public static void handleCloudException(Activity activity, Exception exception, boolean forceRefresh)
    {
        Long accountId = null;
        handleCloudException(activity, accountId, exception, forceRefresh);
    }

    private static void manageException(Context context, boolean forceRefresh)
    {
        if (forceRefresh)
        {
            // ActionManager.actionRequestUserAuthentication(context,
            // SessionUtils.getAccount(context));
        }
        else
        {
            // ActionManager.actionRequestAuthentication(context,
            // SessionUtils.getAccount(context));
        }
    }

}