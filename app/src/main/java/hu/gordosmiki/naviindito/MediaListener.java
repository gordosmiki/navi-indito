package hu.gordosmiki.naviindito;

import android.service.notification.NotificationListenerService;

/**
 * Üres értesítésfigyelő. Csak azért kell, mert az Android ennek az engedélyével
 * enged hozzáférést a médiamunkamenetekhez (szól-e a rádió).
 * Engedély aShellből:
 *   cmd notification allow_listener hu.gordosmiki.naviindito/hu.gordosmiki.naviindito.MediaListener
 */
public class MediaListener extends NotificationListenerService {
}
