const {onValueCreated} = require("firebase-functions/v2/database");
const {setGlobalOptions} = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");

const {GoogleAuth} = require("google-auth-library");
const https = require("https");

const admin = require("firebase-admin");
try {
  admin.initializeApp();
  logger.info("Firebase Admin App Initialized successfully" +
        "(for general context).");
} catch (e) {
  logger.error("CRITICAL: Failed to initialize Firebase Admin App:", e);
}

setGlobalOptions({region: "europe-west1"});

/**
 * Sends an FCM message manually using the HTTP v1 API.
 * @param {string} projectId The Google Cloud Project ID.
 * @param {string} topic The FCM topic to send the message to.
 * @param {object} notificationData The notification payload (e.g., { title: s
 * @param {object} dataPayload The data payload (key-value pairs).
 * @return {Promise<object>} A promise that resolves with an object containing
 */

async function sendFcmManually(
  projectId, topic, notificationData, dataPayload) {
  logger.info(`[sendFcmManually] Preparing to send to topic: ${topic}`);
  try {
    const auth = new GoogleAuth({
      scopes: "https://www.googleapis.com/auth/firebase.messaging",
    });
    const client = await auth.getClient();
    const accessToken = (await client.getAccessToken()).token;

    if (!accessToken) {
      logger.error("[sendFcmManually] Failed to get access token for FCM.");
      return {success: false, error: "Failed to get access token"};
    }
    logger.info("[sendFcmManually] Successfully obtained access token.");

    const fcmMessage = {
      message: {
        topic: topic,
        notification: notificationData,
        data: dataPayload,
      },
    };
    const postData = JSON.stringify(fcmMessage);
    logger.info("[sendFcmManually] FCM Message Payload:", postData);

    const options = {
      hostname: "fcm.googleapis.com",
      port: 443,
      path: `/v1/projects/${projectId}/messages:send`,
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(postData),
      },
    };

    return new Promise((resolve, reject) => {
      const req = https.request(options, (res) => {
        let responseBody = "";
        res.on("data", (chunk) => {
          responseBody += chunk;
        });
        res.on("end", () => {
          logger.info("[sendFcmManually] FCM HTTP API" +
          "Response Status:", res.statusCode);
          logger.info("[sendFcmManually] FCM HTTP API Response Body:",
            responseBody);
          try {
            const parsedBody = JSON.parse(responseBody);
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve({success: true, response: parsedBody});
            } else {
              reject(new Error(`FCM API Error ${res.statusCode}:"+
                '${JSON.stringify(parsedBody)}`));
            }
          } catch (parseError) {
            logger.error("[sendFcmManually]" +
                  "Error parsing FCM response JSON:", parseError);
            reject(new Error(`Error parsing FCM response'+
              '(Status ${res.statusCode}): ${parseError.message}.'+
              ' Raw: ${responseBody.substring(0, 100)}`));
          }
        });
      });
      req.on("error", (e) => {
        logger.error("[sendFcmManually] Error making FCM HTTP request:", e);
        reject(e);
      });
      req.write(postData);
      req.end();
    });
  } catch (error) {
    logger.error("[sendFcmManually] Unexpected error:",
      error.message, error.stack);
    return {success: false, error: error.message};
  }
}


exports.sendChatNotificationV2 = onValueCreated(
  {
    ref: "/messages/{channelId}/{messageId}",
    instance: "chatferit-default-rtdb",
  },
  async (event) => {
    logger.info("----------------------------------------------------");
    logger.info("sendChatNotificationV2 (HTTP API Attempt)" +
            "triggered. Event ID:", event.id);

    const channelId = event.params.channelId;
    const messageId = event.params.messageId;
    const messageData = event.data.val();

    logger.log("Message ID:", messageId, "Channel ID:", channelId);
    logger.log("Message Data:", JSON.stringify(messageData));

    if (!messageData) {
      logger.log("Message data is null for id:", messageId, ". Exiting.");
      return;
    }

    const senderName = messageData.senderName || "New Message";
    const textContent = messageData.message || "";
    const imageUrl = messageData.imageUrl;

    let notificationBody = "";
    if (textContent) {
      notificationBody = `${senderName}: ${textContent}`;
    } else if (imageUrl) {
      notificationBody = `${senderName} sent an image.`;
    } else {
      logger.log("No text or image for message id:",
        messageId, ". Skipping.");
      return;
    }

    const fcmNotificationPart = {
      title: `New message in ${channelId}`,
      body: notificationBody,
    };
    const fcmDataPart = {channelId: channelId, messageId: messageId};
    const topic = `group_${channelId}`;

    try {
      const projectId =
              process.env.GCLOUD_PROJECT || admin.app().options.projectId;
      if (!projectId) {
        logger.error("Project ID could not be determined." +
                  "Cannot send FCM via HTTP.");
        return;
      }
      // eslint-disable-next-line no-unreachable
      logger.info(`Attempting manual FCM send to topic:"+
      "${topic} for project: ${projectId}`);

      const result = await sendFcmManually(projectId,
        topic, fcmNotificationPart, fcmDataPart);
      if (result.success) {
        logger.log("Successfully sent FCM via HTTP API. Topic:",
          topic, "Response:", JSON.stringify(result.response));
      } else {
        logger.error("Failed to send FCM via HTTP API. Topic:",
          topic, "Details:", JSON.stringify(result));
      }
    } catch (error) {
      logger.error("RUNTIME ERROR during manual FCM send process. Topic:",
        topic, "Error:", error.message, "Stack:", error.stack);
    }
    logger.info("----------------------------------------------------");
  },
);
