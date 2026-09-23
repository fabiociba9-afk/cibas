// Service Worker oficial do Firebase Cloud Messaging para Web - ExecutivoGo
importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-messaging-compat.js');

// Configuração oficial do Firebase ExecutivoGo
const firebaseConfig = {
  apiKey: "AIzaSyClWGg3Qrh1qKyBDU3CQgsGK46zX5k2iEo",
  authDomain: "executivogo.firebaseapp.com",
  projectId: "executivogo",
  storageBucket: "executivogo.firebasestorage.app",
  messagingSenderId: "613705889263",
  appId: "1:613705889263:web:6e7d14ee1c85dd7231721a",
  measurementId: "G-EXECUTIVOGO"
};

if (!firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}

const messaging = firebase.messaging();

// Captura mensagens recebidas quando a aba web está em segundo plano ou fechada
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Mensagem recebida em segundo plano:', payload);

  // Se o FCM já incluir o bloco 'notification', o navegador exibe nativamente pelo protocolo WebPush.
  // Evitamos chamar showNotification novamente para não gerar notificação em dobro.
  if (payload.notification) {
    console.log('[firebase-messaging-sw.js] Notificação nativa já processada pelo navegador.');
    return;
  }

  const notificationTitle = payload.data?.title || 'ExecutivoGo';
  const notificationBody = payload.data?.body || 'Nova notificação de viagem ou faturamento.';

  const notificationOptions = {
    body: notificationBody,
    icon: '/assets/logo.png',
    badge: '/assets/logo.png',
    data: payload.data || {},
    vibrate: [200, 100, 200]
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

// Manipula o clique na notificação do navegador
self.addEventListener('notificationclick', (event) => {
  console.log('[firebase-messaging-sw.js] Notificação clicada:', event.notification);
  event.notification.close();

  // Foca na aba aberta do ExecutivoGo ou abre uma nova
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (let i = 0; i < windowClients.length; i++) {
        const client = windowClients[i];
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow('/');
      }
    })
  );
});
