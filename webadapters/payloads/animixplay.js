/**
 * Tenshi payload for animixplay.to
 * for use with the WebAdapter (Activity)
 */
function __tenshi_payload_init() {
    // check if we are on a episode page
    const EPISODE_URL_REGEX = /(?:animixplay.to\/v\d+\/)([^\/]+)(?:\/ep\d+)?/;
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

    // inject payload into player iframe
    var timerId = setInterval(() => {
        // get player frame
        var playerFrame = document.getElementById('iframeplayer');

        // wait until it is loaded
        // we cannot use onload here as the iframe may have already loaded
        if (playerFrame && playerFrame.contentDocument.readyState === 'complete') {
            // notify user
            App.toast('Playback should start shortly');

            // iframe is loaded, inject and execute the payload
            // the payload just grabs the 'video' variable and returns it
            // easy as that
            playerFrame.contentWindow.eval(__payload_for_player_frame.toString());
            var url = playerFrame.contentWindow.__payload_for_player_frame();

            // check we got a url
            if (url !== '') {
                // stop the timer
                clearInterval(timerId);

                // finish
                Tenshi.finish(url);
            }
        }
    }, 100);
}

/**
 * function injected into the player iframe
 */
function __payload_for_player_frame() {
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