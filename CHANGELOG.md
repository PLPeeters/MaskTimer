# Changelog

**1.2.7** (2022-02-06 18:23 CET)

* Fixed list replace action setting an alarm even if a mask is not being worn

**1.2.6** (2022-02-03 09:31 CET)

* Fixed masks still being marked as paused when the user stops wearing a paused mask
* Fixed stop/replace notification actions not working on paused masks
* Mask expiry notifications no longer dismiss on tap
* Swapped surgical and FFP mask expiration preferences
* Switched to NotificationManagerCompat

**1.2.5** (2022-02-02 14:18 CET)

* Fixed mask wear time adjustment causing masks to be marked as not being worn in the database

**1.2.4** (2022-01-30 12:13 CET)

* Saved isPrevious field for masks to the database
* Reset mask alarm on app start
* Check for the exact alarms permission and request it if necessary on Android S and above
* Ensure alarms trigger even when the phone is idle
* Fixed mask addition dialog using an internal class

**1.2.3** (2022-01-27 18:07 CET)

* Restore notifications after the app is updated

**1.2.2** (2022-01-27 09:38 CET)

* Fixed previous mask not being cleared when deleting it
* Improved action animations

**1.2.1** (2022-01-26 18:35 CET)

* Fixed debug variable making it to the release

**1.2.0** (2022-01-26 18:26 CET)

* Moved mask actions to a swipe menu
* Added a hint for the swipe menu when the first mask is added

**1.1.1** (2022-01-24 18:05 CET)

* Fixed expiry notification not being dismissed when stopping the wear of a mask from the main activity

**1.1.0** (2022-01-24 17:49 CET)

* Added a preference to prevent the dismissal of the mask expiration notification
* Added color to notifications
* Fixed incorrect notification text when wear is paused
* Code cleanup

**1.0.1** (2022-01-23 14:05 CET)

* Improved strings

**1.0.0** (2022-01-23 13:07 CET)

* Added context menu option to adjust mask wear time
* Improved theme

**0.3.3** (2022-01-21 12:47 CET)

* Fixed theme issues

**0.3.2** (2022-01-19 17:17 CET)

* Fixed missing translation and worn duration style reset in mask list

**0.3.1** (2022-01-19 12:52 CET)

* Changed the label of the stop wearing button so the layout fits on smaller screens

**0.3.0** (2022-01-19 12:04 CET)

* Added settings to customise the maximum wear time for masks
* Added an action to swap to the previous mask to the timer notification
* Made the duration of expired masks display in bold red

**0.2.1** (2022-01-16 13:17 CET)

* Improved the UI update mechanism

**0.2.0** (2022-01-16 12:47 CET)

* Made the UI update when executing actions via a notification

**0.1.1** (2022-01-16 12:16 CET)

* Made the timer notification restore on device boot
* Made the timer notification silent

