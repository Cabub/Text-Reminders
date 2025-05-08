# SMS Reminder App - Requirements & Functionality Specification

## Project Overview
The SMS Reminder App is designed to help businesses send text message reminders to multiple recipients efficiently. The application follows a three-screen workflow that enables users to compose messages, confirm recipients, and track sending progress.

## Core Functionality Requirements

![App wireframe including all three screens and their actions](wireframe.svg "Wireframe")

### Application Workflow
The application must implement some approximation of the workflow shown in the wireframe:
1. Message composition screen
2. Confirmation screen
3. Sending progress screen

### Required Features
- Message composition and editing
- Multiple recipient entry
- Adding additional recipients dynamically
- Validation of phone numbers
- Confirmation before sending
- Progress tracking during sending
- Ability to cancel the sending process
- Individual message status indicators

## Detailed Screen Requirements

### Screen 1: Message Composition

#### Message Input Area
- Scrollable text field for message composition
- Must support multi-line text
- Should preserve line breaks and formatting
- Optional: Character counter for SMS length tracking

#### Recipients Section
- Input fields for recipient phone numbers
- Initially display 3 empty phone number fields
- Each field should accept and validate phone numbers
- Phone number formatting should be enforced (XXX-XXX-XXXX format)

#### Action Controls
- Add button that creates additional phone number fields
- Send button that advances to the confirmation screen
- Send button should only be active when at least one valid phone number is entered

### Screen 2: Confirmation

#### Message Preview
- Display the exact message that will be sent
- Must be clearly presented as non-editable
- Should maintain all formatting from the composition screen

#### Recipient List
- Display all recipient phone numbers in a list with "To:" header
- Must show all numbers entered on the previous screen
- Should support scrolling if many recipients are added
- Numbers should be displayed in the same order as entered

#### Action Controls
- Cancel button to return to the composition screen
- Send button to initiate sending and advance to progress screen
- All entered data must be preserved when canceling

### Screen 3: Progress

#### Progress Display
- List each recipient with their current message status
- Status indicators must use the following scheme:
    - Success: Check mark icon (✓)
    - In progress: Clock icon (⏱️)
    - Failed: X icon (❌)
- Each row must show the recipient's phone number
- Status must update in real-time as messages are sent

#### Action Controls
- Cancel button to stop remaining messages from being sent
- Okay button to return to the composition screen
- Cancel should stop the process but preserve already sent messages

## Technical Requirements

### SMS Functionality
- The app must use the device's SMS capability to send messages
- Each message must be sent individually to each recipient
- The app must implement a delay between messages (approximately 1 second) to prevent carrier blocking
- The app must track delivery status of messages
- The app must handle permission requests for SMS sending

### User Experience Requirements
- The interface must exactly match the provided wireframe
- The app must provide appropriate feedback during all operations
- The app must handle keyboard appearance appropriately
- The app must be responsive on different screen sizes
- Error states must be handled gracefully with user feedback
- The app must preserve state during screen rotations
- Touch targets must be appropriately sized (minimum 48dp)

### Data Handling
- The message text should be preserved between sessions
- The app should validate phone numbers before sending
- The app should handle various phone number input formats
- User data privacy must be maintained

## Performance Requirements
- The app must remain responsive during the sending process
- The app must efficiently handle large numbers of recipients
- The app must work reliably on Android version 7.0 and higher

## Success Criteria
A successful implementation will
1. Successfully send SMS messages to multiple recipients
2. Provide clear progress tracking
3. Handle all error cases gracefully
4. Operate smoothly without crashes or ANRs
5. Maintain a responsive UI throughout all operations

## Not In Scope
The following features are not required for the initial implementation:
- Scheduled sending
- Message templates
- Contact integration
- Message history
- Read receipts
- MMS functionality
