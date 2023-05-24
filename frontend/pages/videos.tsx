import styles from "@/styles/videos.module.css";
import {ChangeEvent, Component} from "react";
import Link from 'next/link';
import {Button, River, Select, Link as PrimerLink} from '@primer/react-brand'
import {languages, Language} from "@/pages/languages";
import {Heading} from '@primer/react-brand'
import {Image} from '@primer/react-brand'
import {Text} from '@primer/react-brand'

function formatCreatedAt(dateString: string) {
    let date = new Date(dateString);
    let formattedDate = `${date.toLocaleDateString()} at ${date.toLocaleTimeString()}`;
    return formattedDate;
}

interface MyComponentState {
    youtubeId: string,
    videoPreview: { title: string, thumbnailUrl: string }
    selectedLanguage: Language,
    isGenerating: boolean,
    videos: Array<any>
}

export default class Videos extends Component<any, MyComponentState> {
    constructor(props: any) {
        super(props);
        this.state = {
            youtubeId: "XcvhERcZpWw", //"TO0WUTq5zYI"
            videoPreview: { title: "", thumbnailUrl: "" },
            selectedLanguage: { name: "English", code: "eng_Latn" },
            isGenerating: false,
            videos: []
        };

        this.handleGenerateClick = this.handleGenerateClick.bind(this);
        this.setSelectedLanguage = this.setSelectedLanguage.bind(this);
    }

    componentDidMount() {
        this.getVideoPreview()
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

    async getVideoPreview() {
        const token = localStorage.getItem('concise_access_token');

        const response = await fetch(`http://localhost:8080/videos/preview/` + this.state.youtubeId, {
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
                videoPreview: data
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    async handleGenerateClick() {
        this.setState({
            isGenerating: true
        })

        const token = localStorage.getItem('concise_access_token');

        console.time("Create Summary Request Time");
        const response = await fetch(`http://localhost:8080/videos/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                youtubeId: this.state.youtubeId
            })
        });

        if (response.ok) {
            const data = await response.json();
            this.setState({
                isGenerating: false
            })
            console.log(data);
            console.timeEnd("Create Summary Request Time");
            // window.location.replace(`${window.location.origin}/video?videoId=` + data.id);
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
        }
    }

    // These are flores200 (used by NLLB) codes
    setSelectedLanguage = (event: ChangeEvent<HTMLSelectElement>) => {
        const language = languages.find((language: Language) => language.code === event.target.value);
        if (language) {
            this.setState({ selectedLanguage: language });
        }
    }

    // TODO: Add styles and layout
    render() {
        return (
            <div className={styles.container}>
                <Heading>Generate</Heading>
                <div className={styles.generate}>
                    <div className={styles.generateImage}>
                        <Image
                            height={200}
                            aspectRatio="4:3"
                            src={this.state.videoPreview.thumbnailUrl}
                            alt="Thumbnail image for the youtube video that will be generated"
                        />
                    </div>
                    <div className={styles.generateDescription}>
                        <Heading as="h5">{this.state.videoPreview.title}</Heading>
                        <div className={styles.generateDescriptionInputs}>
                            <Select value={this.state.selectedLanguage.code} onChange={this.setSelectedLanguage}>
                                {languages.map((language: Language) => (
                                    <Select.Option key={language.code} value={language.code}>{language.name}</Select.Option>
                                ))}
                            </Select>
                            <Button variant="primary" onClick={this.handleGenerateClick} disabled={this.state.isGenerating}>Generate Summary</Button>
                        </div>
                    </div>
                </div>
                <Heading>History</Heading>
                <div className={styles.videoListContainer}>
                    {this.state.videos.map((video: any) => (
                        <div className={styles.videoListItem} key={video.id}>
                            <div className={styles.videoListItemTitle}>
                                <Image
                                    className={styles.videoListItemImage}
                                    height={50}
                                    aspectRatio="4:3"
                                    src={video.thumbnailUrl}
                                    alt="Thumbnail image for the youtube video"
                                />
                                <PrimerLink href={`/video/?videoId=${video.id}`} className={styles.videoListItemLink}>
                                    {video.title}
                                </PrimerLink>
                            </div>
                            <p>{video.summary} <em>Created {formatCreatedAt(video.createdAt)} </em></p>
                        </div>
                    ))}
                </div>
            </div>
        )
    }
}
