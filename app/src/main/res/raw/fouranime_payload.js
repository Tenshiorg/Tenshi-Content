// Tenshi JS payload for 4anime.to
// injected after every page load using default injector

// get video tag
var vid = document.getElementById('example_video_1_html5_api');

// set onplay event
vid.onplay = function () {
    // stop the video and show poster
    vid.pause();
    vid.setAttribute('poster', 'https://static.wikia.nocookie.net/jimmyneutron/images/7/71/Carl.png');
    vid.autoplay = false;
    vid.load();

    // get the currently playing url
    var url = vid.currentSrc.toString();

    // notify tenshi of the url
    Tenshi.notifyStreamUrl(url);
};

// check window location
var loc = window.location.href;
if (loc.startsWith('https://4anime.to/tonikaku-kawaii-episode')) {
    Tenshi.notifyAnimeSlug('tonikaku-kawaii');
}

// we are now injected
Tenshi.toast("injected!");