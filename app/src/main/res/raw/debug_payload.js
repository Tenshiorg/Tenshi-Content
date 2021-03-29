/**
 * Tenshi payload for genoanime.com
 * for use with the WebAdapter (Activity)
 * 
 * also adds search query support on /search using xquery path parameter
 */
function __tenshi_payload_init() {

    // check window location is geno
    var windowLoc = window.location;
    if (windowLoc.host === 'genoanime.com') {
        // parse query parameters
        var queryParams = new URLSearchParams(windowLoc.search);

        // if we are on the search page
        if (windowLoc.pathname === '/search') {
            // get query parameter 'xquery'
            if (queryParams.has('xquery')) {
                // do the search after a few ms
                var query = queryParams.get('xquery');
                setTimeout(() => {
                    __perform_search(query);
                }, 750);
            }
        }

        // we are on the episode watch page
        if (windowLoc.pathname === '/watch') {
            // parse query parameters
            // geno is so nice to actually just use a query parameter 'name' for their anime pages
            // so we can just kinda take it
            if (queryParams.has('name')) {
                var animeName = queryParams.get('name');
                App.log('found anime name= ' + animeName);
                Tenshi.setPersistentStorage(animeName);
            }

            // do video stuff on geno
            __video_switcharoo_geno();
        }

        // we are "in" the video iframe
        if (windowLoc.pathname.startsWith('/player')) {
            __video_switcharoo_frame();
        }
    } else {
        // we are no longer on geno
        __video_switcharoo_frame();
    }
}

/**
 * perform a anime search
 * @param {String} query the search query
 */
function __perform_search(query) {
    // set value of search bar
    var searchBar = document.getElementById('search-anime');
    searchBar.value = query;

    // manually call search function
    searchAnime();
}

/**
 * does the video stuff
 * 
 * we replace the video iframe with a fixed image.
 * on click, we load into the iframe and grab the url of the video
 * this has to be done like that because the iframe is a different origin (so 'normal' js injection does not work)
 */
function __video_switcharoo_geno() {
    // get container div and iframe elements
    var frameContainer = document.getElementById('video');
    var frame = frameContainer.querySelector('iframe');

    // save the iframe location for later
    const frameSrc = frame.src;

    // TODO we could also just directly load into the iframe
    // maybe this will be a config option in the future?
    //if (directStartPlayback) {
    //    window.location = frameSrc;
    //}

    // yeet the frame
    frame.remove();

    // modify the frame container to have a background image
    frameContainer.style.backgroundColor = '#000000';
    frameContainer.style.backgroundImage = 'url(\'https://storage.googleapis.com/genoanime/genologo.jpg\')';
    frameContainer.style.backgroundSize = '100% 100%';

    // add text into the container
    frameContainer.innerHTML = `<h3 style="position: absolute; top:50%; left:50%; transform: translate(-50%, -50%); color: white; text-align: center">Click here to Play</h3>`;

    // set onclick of the container div
    frameContainer.onclick = function () {
        window.location = frameSrc;
    };
}

/**
 * does the video stuff
 * 
 * we are now "in" the iframe with the video, and quickly grab the video url
 */
function __video_switcharoo_frame() {
    // notify the user
    App.toast('Playback will start shortly...');

    // do this every 500ms until we finish
    // geno seems to sometimes clear the onplay event on the video
    var timerId = setInterval(() => {
        // find all videos and setup listener
        var allVideos = document.querySelectorAll('video');
        allVideos.forEach(video => {
            App.log('setup for video id: ' + video.id);

            // pause the video
            // this improves reliability
            video.pause();

            // setup onplay event to capture url
            video.onplay = function () {
                // stop the video
                video.pause();

                // get the video url and finish
                var vidUrl = video.currentSrc.toString();
                if (vidUrl !== '') {
                    clearInterval(timerId);
                    Tenshi.finish(vidUrl);
                }
            };

            // enable autoplay and start the video
            video.autoplay = true;
            video.play();
        });
    }, 500);
}