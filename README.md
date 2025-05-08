# SMS Reminder App

A simple, efficient Android application for sending batch SMS messages to customers. Perfect for small businesses that need to send payment, appointment, or service reminders and want or need to do so from the store phone.

## Features

- **Bulk SMS Sending**: Enter multiple phone numbers and send the same message to each
- **Individual Sends**: Ensure privacy by sending one-message-per number
- **Pre-configured Template**: Uses a customizable reminder message
- **Simple Interface**: Easy-to-use single screen with text input and send button
- **Progress Tracking**: Shows sending progress for multiple messages

## Installation

### Google Play Store

#### Coming Soon!

### F-Droid

#### Coming Soon!

## Usage

1. Open the app
2. Paste in the message template
3. Type in the phone numbers to send the message to
4. Tap "Send" to begin sending messages
5. The app will send each message with a short delay to prevent carrier blocking

## Default Message Template

```
This is a courtesy reminder about your upcoming appointment/payment. If you have any questions, please contact us.
```

## Development

This app was built as a custom solution for a small business need. The core functionality is intentionally simple:

```kotlin
fun sendMessages(numbers: List<String>) {
    val message = "This is a courtesy reminder about your upcoming appointment/payment. If you have any questions, please contact us."
    
    numbers.forEach { number ->
        sendSMS(number, message)
    }
}
```

## Permissions

This app requires the following permissions:
- SEND_SMS - To send text messages

## Support

For support, please contact the developer directly.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/newfeature`)
3. Commit your changes (`git commit -m 'feat: Add some newfeature'`)
4. Push to the branch (`git push origin feature/newfeature`)
5. Open a Pull Request

## Future Enhancements

- Save message template between runs
- Read receipts
- Auto-fill phone numbers
- Multiple message templates
- Scheduled sending
- Message history tracking
- Contact integration
- Multi-channel sending
- OCR phone number auto-filling
- Statistics tracking
