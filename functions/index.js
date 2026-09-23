const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

/**
 * Cloud Function (1st Gen) acionada quando um novo documento é adicionado na coleção 'notifications'.
 * Envia notificações push reais (FCM) tanto para dispositivos Android quanto para navegadores Web.
 */
exports.sendPushOnNotificationCreated = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    if (!data) return null;

    const {
      userId,
      role,
      companyId,
      title = "ExecutivoGo",
      body = "Você tem uma nova notificação.",
      tripId = "",
      type = "NOTIFICATION",
      fcmToken = ""
    } = data;

    const tokensToSend = new Set();

    if (fcmToken && typeof fcmToken === "string" && fcmToken.trim().length > 10) {
      tokensToSend.add(fcmToken.trim());
    }

    const db = admin.firestore();

    // Se tiver userId específico, busca tokens adicionais salvos no perfil do usuário
    if (userId) {
      try {
        const userDoc = await db.collection("users").doc(userId).get();
        if (userDoc.exists) {
          const userData = userDoc.data() || {};
          if (userData.fcmToken) tokensToSend.add(userData.fcmToken.trim());
          if (userData.fcmTokenAndroid) tokensToSend.add(userData.fcmTokenAndroid.trim());
          if (userData.fcmTokenWeb) tokensToSend.add(userData.fcmTokenWeb.trim());
        }
      } catch (err) {
        console.error(`Erro ao buscar usuário ${userId} para tokens FCM:`, err.message);
      }
    }

    // Se a notificação foi enviada para uma role (ex: ADMIN, DRIVER) sem userId
    if (!userId && role) {
      try {
        const roleUsers = await db.collection("users")
          .where("role", "==", role.toUpperCase())
          .where("active", "==", true)
          .get();

        roleUsers.forEach((doc) => {
          const u = doc.data();
          if (u.fcmToken) tokensToSend.add(u.fcmToken.trim());
          if (u.fcmTokenAndroid) tokensToSend.add(u.fcmTokenAndroid.trim());
          if (u.fcmTokenWeb) tokensToSend.add(u.fcmTokenWeb.trim());
        });
      } catch (err) {
        console.error(`Erro ao buscar usuários da role ${role}:`, err.message);
      }
    }

    // Se a notificação foi enviada para uma empresa (companyId)
    if (!userId && !role && companyId) {
      try {
        const compUsers = await db.collection("users")
          .where("companyId", "==", companyId)
          .where("active", "==", true)
          .get();

        compUsers.forEach((doc) => {
          const u = doc.data();
          if (u.fcmToken) tokensToSend.add(u.fcmToken.trim());
          if (u.fcmTokenAndroid) tokensToSend.add(u.fcmTokenAndroid.trim());
          if (u.fcmTokenWeb) tokensToSend.add(u.fcmTokenWeb.trim());
        });
      } catch (err) {
        console.error(`Erro ao buscar usuários da empresa ${companyId}:`, err.message);
      }
    }

    if (tokensToSend.size === 0) {
      console.log(`[FCM] Nenhum token registrado para notificação ${context.params.notificationId}`);
      return null;
    }

    const payloadData = {
      tripId: String(tripId || ""),
      type: String(type || ""),
      click_action: "FLUTTER_NOTIFICATION_CLICK"
    };

    const messaging = admin.messaging();
    const promises = Array.from(tokensToSend).map(async (token) => {
      try {
        const message = {
          token: token,
          notification: {
            title: title,
            body: body
          },
          data: payloadData,
          android: {
            priority: "high",
            notification: {
              channelId: "executivo_go_trips_channel",
              sound: "default",
              priority: "high",
              defaultVibrateTimings: true,
              icon: "ic_launcher",
              color: "#059669",
              tag: tripId ? `trip_${tripId}` : undefined
            }
          },
          webpush: {
            notification: {
              title: title,
              body: body,
              icon: "/assets/logo.png",
              badge: "/assets/logo_transparent.png",
              tag: tripId ? `trip_${tripId}` : undefined,
              vibrate: [200, 100, 200]
            },
            fcmOptions: {
              link: "/"
            }
          }
        };

        const response = await messaging.send(message);
        console.log(`[FCM] Push enviado com sucesso: ${response}`);
        return response;
      } catch (sendErr) {
        console.error(`[FCM] Falha ao enviar para token:`, sendErr.message);
        return null;
      }
    });

    await Promise.all(promises);
    return null;
  });


