import styles from "@/styles/videos.module.css";
import {Component} from "react";
import Link from 'next/link';

function formatCreatedAt(dateString: string) {
    let date = new Date(dateString);
    let formattedDate = `${date.toLocaleDateString()} at ${date.toLocaleTimeString()}`;
    return formattedDate;
}

export default class Videos extends Component<any, any> {
    constructor(props: any) {
        super(props);
        this.state = {
            videos: []
        };

        this.handleDebugClick = this.handleDebugClick.bind(this);
    }

    componentDidMount() {
        this.fetchAllVideoSummaries();
    }

    async fetchAllVideoSummaries() {
        const token = localStorage.getItem('concise_access_token');

        const response = await fetch(`http://localhost:8080/videos/`, {
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
                videos: data
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    async handleDebugClick() {
        const token = localStorage.getItem('concise_access_token');

        const response = await fetch(`http://localhost:8080/videos/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                youtubeId: "TO0WUTq5zYI"
            })
        });

        if (response.ok) {
            const data = await response.json();
            console.log(data);
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    render() {
        return (
            <div className={styles.container}>
                <div className={styles.navbar}>
                    <p>History</p>
                    <button onClick={this.handleDebugClick}>Generate (debug)</button>
                    <p>Select Language (en)</p>
                </div>
                <div className={styles.videoListContainer}>
                    {this.state.videos.map((video: any) => (
                        <div className={styles.videoListItem} key={video.id}>
                            <Link href={`/video/?videoId=${video.id}`}>
                                <h2><strong>{video.title}</strong></h2>
                            </Link>
                            <p>{video.summary}</p>
                            <p><em>Created {formatCreatedAt(video.createdAt)}</em></p>
                        </div>
                    ))}
                </div>
            </div>
        )
    }
}
