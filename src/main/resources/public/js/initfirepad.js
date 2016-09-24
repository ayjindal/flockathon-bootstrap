function initializeFirepad() {

    var config = {
        apiKey: "AIzaSyBv54ql_TM_z2DW7Vmf7OtvEyzu98kZa1M",
        authDomain: "hyre-8a50b.firebaseio.com/",
        databaseURL: "https://hyre-8a50b.firebaseio.com/"
    };

    firebase.initializeApp(config);
    var firepadRef = getInterviewPadReference();

    var editor = ace.edit("codepad");
    editor.setTheme("ace/theme/tomorrow_night");
    editor.setValue("");
    var session = editor.getSession();
    session.setUseWrapMode(true);
    session.setUseWorker(false);
    session.setMode("ace/mode/java");

    var firepad = Firepad.fromACE(firepadRef, editor);
}

function getInterviewPadUrl() {
    var ref = getInterviewPadReference();
    return baseUrl + "interviewer-view#" + ref.key;
}

// Helper to get hash from end of interview URL or generate a random one.
function getInterviewPadReference() {
    var ref = firebase.database().ref();
    var hash = window.location.hash.replace(/#/g, '');
    if (hash) {
        ref = ref.child(hash);
    } else {
        ref = ref.push();
        window.location = window.location + '#' + ref.key;
    }
    if (typeof console !== 'undefined') {
        console.log('Firebase data: ', ref.toString());
    }
    return ref;
}