import styles from "@/styles/video.module.css";
import {Component} from "react";
import { withRouter, NextRouter } from 'next/router';
import { WithRouterProps } from 'next/dist/client/with-router';
import Link from "next/link";

interface VideoProps extends WithRouterProps {
    router: NextRouter;
}

interface MyComponentState {
    youtubeId: null | string;
    language: null | 'en';
    videoId: null | string,
    video: {
        title: string,
        summary: string,
        chapterDtos: Array<any>
    } | null;
}

function formatSecondsToHHMMSS(seconds: number) {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    let timeString = "";
    if(hrs > 0) {
        timeString += `${hrs.toString().padStart(2, '0')}:`;
    }
    timeString += `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;

    return timeString;
}

class Video extends Component<VideoProps, MyComponentState> {
    static async getInitialProps({ query }: any) {
        const { youtubeId, language, videoId } = query;
        return { youtubeId, language, videoId };
    }

    constructor(props: any) {
        super(props);
        this.state = {
            youtubeId: null,
            language: null,
            videoId: null,
            video: null
        };
    }

    // When the user clicks the extension icon they are redirected to this component (/video) with
    // appropriate query parameters in the url bar (youtubeId and youtubeId). This component will
    // send a search request to the backend and load the video if it already exists. The other case
    // for a user landing on this page is when they click a link from their video history. In this case,
    // have the video ID.

    componentDidMount() {
        const { router } = this.props;
        const youtubeId = router.query["youtubeId"] as string; // TO0WUTq5zYI
        const language = router.query["language"] as 'en';
        const videoId = router.query["videoId"] as string;

        console.log(youtubeId, language, videoId)

        if (videoId) {
            this.setState({
                ...this.state,
                videoId
            });
            this.fetchVideoById(videoId);
        } else {
            // Fetch videos by youtubeId and language
            this.setState({
                ...this.state,
                youtubeId,
                language
            });
            this.fetchVideos(youtubeId, language);
        }
    }

    async fetchVideoById(videoId: string) {
        const token = localStorage.getItem('concise_access_token');

        const response = await fetch(`http://localhost:8080/videos/${videoId.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            console.log(data);
            this.setState({
                ...this.state,
                video: data
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    async fetchVideos(youtubeId: string, language: string) {
        const token = localStorage.getItem('concise_access_token');

        const queryParams = new URLSearchParams({
            youtubeId: youtubeId,
            language: language
        });

        const response = await fetch(`http://localhost:8080/videos/search?${queryParams.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            console.log(data);
            this.setState({
                ...this.state,
                video: data.length >= 1 ? data[0] : null
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    // TODO: Properly linkify chapter timestamps
    render() {
        console.log(this.props)
        return (
            <div className={styles.container}>
                <div className={styles.navbar}>
                    <Link href={`/videos`}>
                        <p>Back</p>
                    </Link>
                </div>
                <div className={styles.fullSummary}>
                    <p>{this.state.youtubeId}</p>
                    <p>{this.state.language}</p>
                    <h1><strong>{this.state.video?.title}</strong></h1>
                    <p>{this.state.video?.summary}</p>
                </div>
                {this.state.video?.chapterDtos.map((chapterDto: any) => (
                    <div className={styles.chapterContainer} key={chapterDto.id}>
                        <a className={styles.videoStartTimeLink} href=""><h2><strong>{chapterDto.title}</strong></h2>
                            {formatSecondsToHHMMSS(chapterDto.startTimeSeconds)}
                        </a>
                        <p>{chapterDto.summary}</p>
                    </div>
                ))}
            </div>
        )
    }
}

export default withRouter(Video);