// Tenshi JS payload for 4anime.to
// injected after every page load using default injector
// -- constants --
const SLUG_REGEX = /(?:4anime.to\/)(.+)(?:-episode-\d+)(?:\?id=)?/;
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
var allVideos = document.querySelectorAll('video');
for (var i = 0; i < allVideos.length; i++) {
    var video = allVideos[i];
    App.log('setup for video with id: ' + video.id);

    // disable autoplay on the video
    video.autoplay = false;

    // setup onplay event to capture the url
    video.onplay = function () {
        // stop the video and force show poster
        video.pause();
        video.setAttribute('poster', VIDEO_POSTER);
        video.autoplay = false;
        video.load();

        // get the url from the video and notify tenshi
        var vidUrl = video.currentSrc.toString();
        if (vidUrl !== '') {
            Tenshi.onStreamUrlFound(vidUrl);
        }
    };
}

// if we setup a video, notify the user they can start it now
if (allVideos.length > 0) {
    App.toast('Start the Video now.');
}

// notify user we are successfully injected
App.toast('Injected!');