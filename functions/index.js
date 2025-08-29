const {onValueCreated} = require("firebase-functions/v2/database");
const {setGlobalOptions} = require("firebase-functions/v2");
const logger = require("firebase-functions/logger"); // Built-in logger for v2

const {GoogleAuth} = require("google-auth-library");
const https = require("https");

const admin = require("firebase-admin");

try {
  if (admin.apps.length === 0) {
    admin.initializeApp();
    logger.info("Firebase Admin App Initialized successfully.");
  } else {
    logger.info("Firebase Admin App already initialized.");
  }
} catch (e) {
  logger.error("CRITICAL: Failed to initialize Firebase Admin App:", e);
}

setGlobalOptions({region: "europe-west1"});
/**
 * Sends an FCM message manually using the HTTP v1 API.
 * This function seems to be from your existing codebase.
 * @param {string} projectId The Google Cloud Project ID.
 * @param {string} target The FCM target (e.g. topic name or device token).
 * @param {object} notificationPayload The notification part of the FCM message.
 * @param {object} dataPayload The data part of the FCM message.
 * @param {boolean} isTopic If the target is a topic or a device token.
 * @return {Promise<object>} A promise that resolves with the API response.
 */
async function sendFcmManually(
  projectId,
  target,
  notificationPayload,
  dataPayload,
  isTopic = true,
) {
  logger.info("[sendFcmManually] Preparing to send." +
        "Target: ${target}, Is Topic: ${isTopic}");
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
        notification: notificationPayload,
        data: dataPayload,
      },
    };

    if (isTopic) {
      fcmMessage.message.topic = target;
    } else {
      fcmMessage.message.token = target;
    }

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
          logger.info("[sendFcmManually] FCM HTTP API Response Status:",
            res.statusCode);
          logger.info("[sendFcmManually] FCM HTTP API Response Body:",
            responseBody.substring(0, 500));
          try {
            const parsedBody = JSON.parse(responseBody);
            if (res.statusCode >= 200 && res.statusCode < 300) {
              resolve({success: true, response: parsedBody});
            } else {
              const errorMessage = `FCM API Error ${res.statusCode}:` +
                    `${JSON.stringify(
                      parsedBody.error ?
                        parsedBody.error.message : parsedBody)}`;
              logger.error("[sendFcmManually] Error from FCM API:",
                errorMessage);
              reject(new Error(errorMessage));
            }
          } catch (parseError) {
            logger.error("[sendFcmManually] Error parsing FCM response JSON:",
              parseError, "Raw Body:", responseBody.substring(0, 500));
            reject(
              new Error("Error parsing FCM response" +
                      ` (Status ${res.statusCode}):` +
                      ` ${parseError.message}. Raw: ` +
                      `${responseBody.substring(0, 100)}`));
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
    logger.error("[sendFcmManually] Unexpected error in sendFcmManually:",
      error.message, error.stack);
    return {
      success: false, error:
              `Critical error in sendFcmManually: ${error.message}`,
    };
  }
}


exports.sendChatNotification = onValueCreated(
  {
    ref: "/messages/{channelId}/{messageId}",
    instance: "chatferit-default-rtdb",
  },
  async (event) => {
    logger.info("----------------------------------------------------");
    logger.info("sendChatNotification triggered. Event ID:", event.id);

    const channelId = event.params.channelId;
    const messageId = event.params.messageId;
    const messageData = event.data.val();

    logger.info(`Message ID: ${messageId}, Channel ID: ${channelId}`);
    logger.info("Raw Message Data from DB:", JSON.stringify(messageData));

    if (!messageData) {
      logger.warn("Message data is null for id:"
        , messageId, ". Exiting function.");
      return;
    }

    const senderIdFromDB = messageData.senderId;
    const senderNameFromDB = messageData.senderName || "New Message";
    const encryptedMessageForRecipientFromDB =
          messageData.encryptedMessageForRecipient;
    const imageUrlFromDB = messageData.imageUrl;

    let systemNotificationBodyText;
    if (encryptedMessageForRecipientFromDB) {
      systemNotificationBodyText = `${senderNameFromDB} sent you a message.`;
      if (imageUrlFromDB) {
        systemNotificationBodyText = `${senderNameFromDB} sent an image.`;
      }
    } else if (messageData.plainTextMessage) {
      systemNotificationBodyText = `${senderNameFromDB}: ` +
            `${messageData.plainTextMessage.substring(0, 100)}`;
      if (imageUrlFromDB) {
        systemNotificationBodyText = `${senderNameFromDB} sent an image:` +
              ` ${messageData.plainTextMessage.substring(0, 50)}`;
      }
    } else if (imageUrlFromDB) {
      systemNotificationBodyText = `${senderNameFromDB} sent an image.`;
    } else {
      logger.warn("No suitable content (encrypted, plain, or image)" +
            " for system notification body. Message ID:", messageId);
      systemNotificationBodyText = `${senderNameFromDB} sent a new message.`;
    }

    const fcmNotificationPart = {
      title: senderNameFromDB,
      body: systemNotificationBodyText,
    };

    const fcmDataPart = {
      channelId: channelId,
      senderName: senderNameFromDB,
      encryptedMessageForRecipient: encryptedMessageForRecipientFromDB || "",
      senderId: senderIdFromDB || "",

      messageId: messageId,
      notificationType: "NEW_MESSAGE",
    };

    if (!fcmDataPart.encryptedMessageForRecipient &&
          !messageData.plainTextMessage && !imageUrlFromDB) {
      logger.error("CRITICAL: No content (encrypted, plain, or image)" +
            " to send in data payload for message ID:"
      , messageId, "Skipping FCM send.");
      return;
    }
    if (!fcmDataPart.senderId) {
      logger.warn("senderId is missing from database message. " +
      "Client navigation might be impaired. Message ID:", messageId);
    }
    if (!fcmDataPart.channelId) {
      logger.error(
        "CRITICAL: channelId is missing (should come from event.params)." +
            " This should not happen. Skipping FCM send.");
      return;
    }


    const topicTarget = `group_${channelId}`;

    try {
      const projectId =
            process.env.GCLOUD_PROJECT || (admin.app().options.projectId);
      if (!projectId) {
        logger.error("Project ID could not be determined." +
              " Cannot send FCM via HTTP API.");
        return;
      }

      logger.info(
        `Attempting FCM send to TOPIC: ${topicTarget}` +
            ` for project: ${projectId}`);
      logger.info("Data Payload being sent (fcmDataPart):"
        , JSON.stringify(fcmDataPart));
      logger.info("Notification Payload being sent (fcmNotificationPart):"
        , JSON.stringify(fcmNotificationPart));


      const result = await sendFcmManually(
        projectId,
        topicTarget,
        fcmNotificationPart,
        fcmDataPart,
        true,
      );

      if (result.success) {
        logger.info("Successfully sent FCM via HTTP API. Target:",
          topicTarget, "Response:", JSON.stringify(result.response));
      } else {
        logger.error("Failed to send FCM via HTTP API. Target:",
          topicTarget, "Details:", JSON.stringify(result));
      }
    } catch (error) {
      logger.error("RUNTIME ERROR during FCM send process. Target:",
        topicTarget, "Error:", error.message, "Stack:", error.stack);
    }
    logger.info(`sendChatNotification finished for Event ID: ${event.id}`);
    logger.info("----------------------------------------------------");
  },
);
