/**
 * Tenshi payload for 4anime.to
 * for use with the WebAdapter (Activity)
 */
function __tenshi_payload_init() {
    const EPISODE_URL_REGEX = /(?:4anime.to\/)(.+)(?:-episode-\d+)(?:\?id=)?/;

    // check if we are on a episode page
    var locMatch = EPISODE_URL_REGEX.exec(window.location.href);

    // if we are not, exit function
    if (locMatch == null) {
        return;
    }

    // otherwise, get slug from capture group and save it
    var slug = locMatch[1];
    if (slug !== '') {
        App.log('found slug for location ' + window.location.href + ' : ' + slug);
        Tenshi.setPersistentStorage(slug);
    }

    // setup all videos on the page for capture
    var allVideos = document.querySelectorAll('video');
    allVideos.forEach(vid => {
        // disable autoplay on the video
        App.log('setup for video with id: ' + vid.id);
        vid.autoplay = false;
        vid.pause();

        // setup onplay event to capture the url
        vid.onplay = function () {
            // stop the video
            vid.pause();

            // get the url from the video
            var vidUrl = vid.currentSrc.toString();
            if (vidUrl !== '') {
                Tenshi.finish(vidUrl);
            }
        }
    });

    // if we setup at leas one video, notify user to start the video
    if (allVideos.length > 0) {
        App.toast('Start the video now');
    }
}