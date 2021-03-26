// Tenshi JS payload for yugenani.me
// injected after every page load using default injector

// -- grab infos for persistent storage --
const PERSISTENT_REGEX = /(?:yugenani.me\/watch\/)(\d+\/.+)(?:\/\d+)/;

var windowLoc = window.location.href;
var match = PERSISTENT_REGEX.exec(windowLoc);
if (match != null) {
    var persistentStorage = match[1];
    App.log('PERSISTENT_REGEX match for location ' + windowLoc + ' is: ' + persistentStorage);
    if (persistentStorage !== '') {
        Tenshi.setPersistentStorage(persistentStorage);
    }
}

// -- grab video url --
// get main embed
var mainEmbed = document.getElementById('main-embed');

if (mainEmbed != null) {
    App.toast('Playback will start shortly...');
    setTimeout(function () {
        // run query selector inside the embed iframe
        var allVideos = mainEmbed.contentWindow.document.body.querySelectorAll('video');
        for (var i = 0; i < allVideos.length; i++) {
            var video = allVideos[i];
            App.log('setup for video with id: ' + video.id);

            // disable autoplay on the video
            video.autoplay = false;

            // grab the url from the video (page doesn't have episode selection anyways...)
            var vidUrl = video.currentSrc.toString();
            if (vidUrl !== '') {
                Tenshi.onStreamUrlFound(vidUrl);
            }
        }
    }, 1000);
} else {
    App.logE('main-embed not found');
}