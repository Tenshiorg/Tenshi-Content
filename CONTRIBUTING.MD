# Contributing

# Prerequisites

Before you start, please note that you should be able to use the following technologies. Existing contributors will not actively teach them to you.

- Basic Android developement
- Java
- Web Scraping
    - HTML
    - JavaScript*
    - JSoup*

\* maybe not required depending on your implementation

## Tools

- [Android Studio](https://developer.android.com/studio)
- Emulator or phone ready for development and Tenshi installed
- Google Chrome on the host machine (useful when debugging Web Adapters)


# Getting Help

Theres currently no discord or anything setup, so please refer to existing Content Adapters for examples.<br>
If required, you can also write a issue

# Writing a Content Adapter

Content Adapters are a way to provide content, like episode streams, to Tenshi.<br>
Adapters are implemented as Android Services with a [AIDL](https://developer.android.com/guide/components/aidl) interface.


There are two ways of writing a Content Adapter:

# Web Adapters

A Web Adapter is essentially just a WebView with Javascript injected into the page. <br>
A Web Adapter consists of a [json definition](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/webadapters/adapter-definitions.json) and a [payload](https://github.com/Tenshiorg/Tenshi-Content/tree/kohai/webadapters/payloads) that is injected into the page.


To start developing a Web Adapter, clone this repository and set [DEBUG_MODE](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/app/src/main/java/io/github/shadow578/tenshicontent/webadapter/Constants.java#L14) to true. <br>

## Definition

The definition of a Web Adapter is loaded every time the WebAdapterService is initialized.<br>
When in debug mode, write your definition in the file in [raw/debug_definition.json](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/app/src/main/res/raw/debug_definition.json).


### Format

```json
[
  {
    "name": "fouranime.web",
    "displayName": "4Anime",
    "storagePattern": null,
    "searchUrl": "https://4anime.to/?s=%s",
    "episodeUrl": "https://4anime.to/%s-episode-%02d",
    "payload": "webadapters/payloads/4anime.json",
    "userAgentOverride": null,
    "domStorageEnabled": null,
    "allowContentAccess": null
  }
]
```

Property    | Description
------------|------------
name        | a unique name for your adapter
displayName | the name shown to the user
storagePattern  | a regex pattern to validate the persistent storage. null to disable
searchUrl       | search url used when persistent storage is empty. %s is replaced with the name of the anime, url- escaped
episodeUrl      | url to directly go to a episode. %s is replaced with the contents of persistent storage, %d with the episode number
payload         | javascript payload of this adapter, relative to [PAYLOAD_ROOT](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/app/src/main/java/io/github/shadow578/tenshicontent/webadapter/Constants.java)
userAgentOverride   | controls the [user agent](https://developer.android.com/reference/android/webkit/WebSettings#setUserAgentString(java.lang.String)) of the webview. left default if null
domStorageEnabled   | controls [dom storage](https://developer.android.com/reference/android/webkit/WebSettings#setDomStorageEnabled(boolean)). left default if null
allowContentAccess  | controls [content access](https://developer.android.com/reference/android/webkit/WebSettings#setAllowContentAccess(boolean)). left default if null


## Payload

The payload of a Web Adapter is loaded every time it is injected into the page.<br>
When in debug mode, write your definition in the file in [raw/debug_payload.js](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/app/src/main/res/raw/debug_payload.js). 


Writing the payload is, of course, highly dependent on the page you are writing it for. <br>
See [webadapter/payloads](https://github.com/Tenshiorg/Tenshi-Content/tree/kohai/webadapters/payloads) for examples.

The general goals:
- write something to persistent storage that, in combination with the episodeUrl of the definition, gets the user to the episode page as direct as possible
- get the video url from the page
- block (disruptive) ads

### Javascript interface

The following functions are accessible to Payloads:

App.            | Description
----------------|-------------
toast(String)   | make a toast
log(String)     | write a message to Log.d, tag "JSInterface"
logE(String)    | write a message to Log.e, tag "JSInterface"

\* see [JSInterface](https://github.com/Tenshiorg/Extensions-Lib/blob/kohai/extensionsLib/src/main/java/io/github/shadow578/tenshi/extensionslib/content/util/WebViewAdapterActivity.java#L178)


Tenshi.                 | Description
------------------------|-------------
getUniqueName()         | the current unique name
getAnimeTitle()         | the (english) anime title
getAnimeTitleJp()       | the (japanese) anime title
getMalId()              | the MAL id of the anime
getPersistentStorage()  | get the contents of persistent storage
setPersistentStorage(String)  | set the contents of persistent storage
finish(String)          | closes the webview and forwards the argument as stream url to Tenshi

\* see [WebAdapterJs](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/app/src/main/java/io/github/shadow578/tenshicontent/webadapter/WebAdapterActivity.java#L211)


## Debugging

When running a debug build with DEBUG_MODE enabled, the WebAdapterActivity will enable [Web Contents Debugging](https://developer.android.com/reference/android/webkit/WebView#setWebContentsDebuggingEnabled(boolean)), allowing you to (kinda) open full Chrome DevTools on your webview by opening chrome://inspect on your host PC with the phone or emulator connected to it.

Quick Note: Chrome is (at least for me) sometimes very slow to connect to the webview. So bring patience when debugging.


# Native Adapters

A native adapter gives you more control over how things work, and also allows you to create a adapter without a ui (or a custom one). <br>

The most barebone Content Adapter just extends [Service](https://developer.android.com/reference/android/app/Service) and returns a implementation of [IContentAdapter.Stub](https://github.com/Tenshiorg/Extensions-Lib/blob/kohai/extensionsLib/src/main/aidl/io/github/shadow578/tenshi/extensionslib/content/IContentAdapter.aidl) in onBind().

If you need a activity for your adapter, you can use a [ActivityAdapterService](https://github.com/Tenshiorg/Extensions-Lib/blob/kohai/extensionsLib/src/main/java/io/github/shadow578/tenshi/extensionslib/content/util/ActivityAdapterService.java).


After writing your Content Adapter Service, you have to add it to your manifest like so:

```xml
<service
    android:name=".webadapter.WebAdapterService"
    android:exported="true">

    <intent-filter>
        <action android:name="io.github.shadow578.tenshi.content.ADAPTER" />
        <category android:name="io.github.shadow578.tenshi.content.ADAPTER" />
    </intent-filter>

    <meta-data
        android:name="io.github.shadow578.tenshi.content.ADAPTER_VERSION"
        android:value="2" />
</service>
```

the service __must__:
- be exported (android:exported="true")
- define a intent filter with action and category set to "io.github.shadow578.tenshi.content.ADAPTER"
- contain meta-data "io.github.shadow578.tenshi.content.ADAPTER_VERSION" with value set to the same value as in the used extension lib (see IContentAdapter or Constants).


## Debugging

Debugging of Content Adapters is easiest done using the [TestActivity](https://github.com/Tenshiorg/Tenshi-Content/blob/kohai/app/src/main/java/io/github/shadow578/tenshicontent/TestActivity.java). This uses the same logic to bind the adapters and allows for you to set breakpoints.
