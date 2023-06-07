import '@/styles/globals.css'
import '@primer/react-brand/lib/css/main.css'
import '@primer/react-brand/fonts/fonts.css'

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

  function authUser(youtubeId: string | null) {
    const bearerToken = localStorage.getItem('concise_access_token');

    if (bearerToken) {
      if (youtubeId == null) {
        router.push('/videos');
      } else {
        router.push('/videos' + '?youtubeId=' + youtubeId);
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
        const youtubeId = extractYouTubeVideoId(event.data.payload);
        authUser(youtubeId)
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
