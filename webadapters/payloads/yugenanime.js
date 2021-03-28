/**
 * Tenshi payload for yugenani.me
 * for use with the WebAdapter (Activity)
 */
function __tenshi_payload_init() {
    // start removing ads
    __ad_removal();

    // check if we are on a episode page
    const EPISODE_URL_REGEX = /(?:yugenani.me\/watch\/)(\d+\/.+)(?:\/\d+)/;
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

    // get the main embed
    var mainEmbed = document.getElementById('main-embed');
    if (mainEmbed != null) {
        var timerId = setInterval(() => {
            // run query selector in the iframe (same origin)
            var allVideos = mainEmbed.contentWindow.document.body.querySelectorAll('video');
            allVideos.forEach(video => {
                // disable autoplay on the video
                App.log('setup for video with id: ' + video.id);
                video.autoplay = false;
                video.pause();

                // grab the url from the video
                // we don't have to care about waiting for onplay as the page doesn't have episode selection anyways
                var vidUrl = video.currentSrc.toString();
                if (vidUrl !== '') {
                    // stop timer
                    clearInterval(timerId);

                    // finish
                    Tenshi.finish(vidUrl);
                }
            });
        }, 500);

        // notify user
        App.toast('Playback should start shortly...');
    }
}

/**
 * removes all iframes that are not inside the <body> tag
 */
function __ad_removal() {
    // check we are on the right page
    if (window.location.host !== 'yugenani.me') {
        return;
    }

    // ad remover
    setInterval(() => {
        // query all iframes on the page
        var allIFrames = document.querySelectorAll('iframe');
        allIFrames.forEach(iframe => {
            // is this iframe outside the body tag?
            if (!document.body.contains(iframe)) {
                iframe.remove();
            }
        });
    }, 100);
}