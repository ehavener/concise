document.addEventListener('DOMContentLoaded', () => {
    const handleMessage = (event) => {
        // Check if the message is from the Next.js app
        if (event.data.type === 'nextAppLoaded') {
            setTimeout(() => {
                const iframe = document.getElementById('nextApp');

                chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
                    const activeTab = tabs[0];
                    const message = {
                        type: 'url',
                        payload: activeTab.url,
                    };

                    // Send the current URL to the Next.js app
                    iframe.contentWindow.postMessage(message, '*');
                });
            }, 1)
        }
    };

    // Add an event listener for the message event
    window.addEventListener('message', handleMessage);

    // Remove the event listener when the popup is closed
    window.addEventListener('beforeunload', () => {
        window.removeEventListener('message', handleMessage);
    });
});
