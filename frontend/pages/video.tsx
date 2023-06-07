import styles from "@/styles/video.module.css";
import {Component} from "react";
import { withRouter, NextRouter } from 'next/router';
import { WithRouterProps } from 'next/dist/client/with-router';
import Link from "next/link";
import {Button} from '@primer/react-brand'
import { ChevronLeftIcon } from '@primer/octicons-react';

import dynamic from 'next/dynamic';

const LoadingDots = dynamic(
    () => import('@/pages/LoadingDots'),
    { ssr: false } // This line will prevent component from rendering on the server.
);

interface VideoProps extends WithRouterProps {
    router: NextRouter;
}

interface VideoState {
    videoId: null | string,
    video: {
        title: string,
        summary: string,
        chapterDtos: Array<any>
    } | null,
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

class Video extends Component<VideoProps, VideoState> {
    static async getInitialProps({ query }: any) {
        const { videoId } = query;
        return { videoId };
    }

    constructor(props: any) {
        super(props);
        this.state = {
            videoId: null,
            video: null,
        };
    }

    componentDidMount() {
        const { router } = this.props;
        const videoId = router.query["videoId"] as string;
        this.setState({
            ...this.state,
            videoId
        });
        this.fetchVideoById(videoId);
        this.pollUntilSummariesGenerated(videoId);
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
            this.setState({
                ...this.state,
                video: data
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    pollUntilSummariesGenerated(videoId: string) {
        const token = localStorage.getItem('concise_access_token');

        fetch(`http://localhost:8080/videos/${videoId.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        }).then(response => response.json())
            .then(data => {
                // Process the received updates
                this.setState({
                    ...this.state,
                    video: data
                })
                // Schedule the next poll after a delay
                const videoSummaryCreated = this.state.video?.summary;
                const chapterSummariesCreated = this.state.video?.chapterDtos.every(c => c.summary)
                if (!videoSummaryCreated || !chapterSummariesCreated) {
                    setTimeout(() => {
                        this.pollUntilSummariesGenerated(videoId)
                    }, 5000); // Poll every 5 seconds
                }
            })
            .catch(error => {
                console.error('Error fetching updates:', error);
            });
    }

    // TODO: Properly linkify chapter timestamps
    // TODO: Improve styles and layout
    render() {
        return (
            <div className={styles.container}>
                <div className={styles.navbar}>
                    <Link href={`/videos`}>
                        <Button variant="subtle" hasArrow={false}><ChevronLeftIcon /> Back</Button>
                    </Link>
                </div>
                <div className={styles.fullSummary}>
                    <h1><strong>{this.state.video?.title}</strong></h1>
                    <p>{this.state.video?.summary ? this.state.video?.summary : <LoadingDots />}</p>
                </div>
                {this.state.video?.chapterDtos.map((chapterDto: any) => (
                    <div className={styles.chapterContainer} key={chapterDto.id}>
                        <a className={styles.videoStartTimeLink} href=""><h2><strong>{chapterDto.title}</strong></h2>
                            {formatSecondsToHHMMSS(chapterDto.startTimeSeconds)}
                        </a>
                        { chapterDto.summary ? <p>{chapterDto.summary}</p> : <LoadingDots />}
                    </div>
                ))}
            </div>
        )
    }
}

export default withRouter(Video);