// Tenshi JS payload for genoanime.com
// injected after every page load using default injector

// -- search functionality --
function __perform_search(query) {
    // set value of search bar
    var searchBar = document.getElementById('search-anime');
    searchBar.value = query;

    // manually call search function
    searchAnime();
}

// if we are on the search page
var windowLoc = window.location;
if (windowLoc.host === 'genoanime.com') {
    if (windowLoc.pathname === '/search') {
        // parse query parameters
        var queryParams = new URLSearchParams(windowLoc.search);

        // get query parameter 'xquery'
        if (queryParams.has('xquery')) {
            var query = queryParams.get('xquery');

            // do the search after a short delay
            setTimeout(() => {
                __perform_search(query);
            }, 750);
        }
    }


    if (windowLoc.pathname === '/watch') {
        // -- persistent storage --
        // parse query parameters
        var queryParams = new URLSearchParams(windowLoc.search);

        // geno is so nice to actually just use a query parameter 'name' for their animes
        // so we just kinda have to take it
        if (queryParams.has('name')) {
            var animeName = queryParams.get('name');
            App.log('found anime name= ' + animeName);
            Tenshi.setAnimeName(animeName);
        }


        // -- grab video url --
        // we replace the video iframe with a fixed image
        // on click, we load into the iframe and grab the url
        // we have to do it this way because the iframe is on a different origin (so injecting js does not work)

        // get container div and iframe elements
        var frameContainer = document.getElementById('video');
        var frame = frameContainer.querySelector('iframe');

        // get the iframe location for later
        const frameSrc = frame.src;

        // override the whole onclick and layout stuff if we should directly start playback
        // (this is a setting of the adapter)
        if (Tenshi.shouldDirectlyStartPlayback()) {
            window.location = frameSrc;
        }

        // bye- bye iframe ;)
        frame.remove();

        // modify the frame container to have a background image
        frameContainer.style.backgroundColor = '#000000';
        frameContainer.style.backgroundImage = 'url(\'https://storage.googleapis.com/genoanime/genologo.jpg\')';
        frameContainer.style.backgroundSize = '100% 100%';

        // add text into the container
        frameContainer.innerHTML = `
            <h3 style="position: absolute; top:50%; left:50%; transform: translate(-50%, -50%); color: white; text-align: center">Click here to Play</h3>    
        `;

        // set onclick of the container div
        frameContainer.onclick = function () {
            window.location = frameSrc;
        };
    }
} else {
    // -- grab video url, part 2 --
    // if we are no longer on genoanime.com, but reloaded "into" the iframe

    // notify that we are injected
    //App.toast('injected stage 2!');
    App.toast('playback will start shortly...');

    // find all videos and setup listener
    var allVideos = document.querySelectorAll('video');
    for (var i = 0; i < allVideos.length; i++) {
        var video = allVideos[i];
        App.log('setup for video with id: ' + video.id);

        // enable autoplay on the video
        video.autoplay = true;

        // setup onplay event to capture the url
        video.onplay = function () {
            // stop the video and force show poster
            video.pause();
            //video.setAttribute('poster', '');
            video.autoplay = false;
            video.load();

            // get the url from the video and notify tenshi
            var vidUrl = video.currentSrc.toString();
            if (vidUrl !== '') {
                Tenshi.onStreamUrlFound(vidUrl);
            }
        };
    }
}

