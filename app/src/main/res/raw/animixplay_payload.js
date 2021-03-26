// Tenshi JS payload for animixplay.to
// injected after every page load using default injector
// -- constants --
const SLUG_REGEX = /(?:animixplay.to\/v\d+\/)([^\/]+)(?:\/ep\d+)?/;
const VIDEO_POSTER = '';

// -- update slug in persistent storage --
var windowLoc = window.location.href;
var slugMatch = SLUG_REGEX.exec(windowLoc);
if (slugMatch != null) {
    var slug = slugMatch[1];
    App.log('SLUG_REGEX match for location ' + windowLoc + ' is: ' + slug);
    if (slug !== '') {
        Tenshi.setSlug(slug);
    }
}

// -- setup all videos on the page for capturing --
// payload injected into the iframe
function __payload_for_player_iframe() {
    // notify that we are injected
    //App.toast('injected stage 2!');

    // return video or backup (if main url is empty)
    if (video !== '') {
        return video;
    }

    // main video is empty, take the backup
    Tenshi.log('main video is empty, taking backup');
    return backup;
}

// inject into player's iframe
var timerId = setInterval(() => {
    // get player iframe
    var playerIFrame = document.getElementById('iframeplayer')

    // wait until it is loaded
    // can't use onload here as it may already be loaded
    if (playerIFrame && playerIFrame.contentDocument.readyState == 'complete') {
        // notify user playback will start soon
        App.toast('Playback will start shortly');

        // ready, inject payload and execute
        // the payload will return the url from the iframe context
        playerIFrame.contentWindow.eval(__payload_for_player_iframe.toString());
        var url = playerIFrame.contentWindow.__payload_for_player_iframe();

        // check we have a url
        if (url !== '') {
            // stop the interval call
            clearInterval(timerId);

            // callback main app
            Tenshi.onStreamUrlFound(url);
        }
    }
}, 100);

// notify user we are successfully injected
//App.toast('Injected stage 1!');