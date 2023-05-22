import '@/styles/globals.css'
import type { AppProps } from 'next/app'
import {useEffect} from "react";
import {useRouter} from "next/router";

export default function App({ Component, pageProps }: AppProps) {
  const router = useRouter();

  function extractYouTubeVideoId(url: string) {
    const regex = /^(?:https?:\/\/)?(?:www\.)?(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=))((\w|-){11})(?:\S+)?$/;
    const matches = url.match(regex);

    return matches && matches[1];
  }

  function authUser(videoId: string | null) {
    const bearerToken = localStorage.getItem('concise_access_token');

    if (bearerToken) {
      if (videoId == null) {
        // Route user to home (history list view)
        router.push('/videos');
      } else {
        // Route user to VideoDetail, fetch videoId
        router.push('/video' + '?language=en&youtubeId=' + videoId);
      }
    } else {
      // Route user to login
      console.log('No bearer token found in localStorage.');
      router.push('/login');
    }
  }

  useEffect(() => {
    // Send a message to the parent (popup) when the app is initialized
    window.parent.postMessage({ type: 'nextAppLoaded' }, '*');

    const handleMessage = (event: any) => {
      // Check if the message is the URL sent from the popup
      if (event.data.type === 'url') {
        // Perform any action you need with the received URL
        const videoId = extractYouTubeVideoId(event.data.payload);
        authUser(videoId)
      }
    };

    // Add an event listener for the message event
    window.addEventListener('message', handleMessage);

    // Clean up the event listener when the component is unmounted
    return () => {
      window.removeEventListener('message', handleMessage);
    };
  }, []);

  return <Component {...pageProps} />
}
