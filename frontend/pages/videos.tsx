import styles from "@/styles/videos.module.css";
import {ChangeEvent, Component} from "react";
import {Button, River, Select, Link as PrimerLink} from '@primer/react-brand'
import {languages, Language} from "@/pages/languages";
import {Heading} from '@primer/react-brand'
import {Image} from '@primer/react-brand'
import {WithRouterProps} from "next/dist/client/with-router";
import {NextRouter, withRouter} from "next/router";

function formatCreatedAt(dateString: string) {
    let date = new Date(dateString);
    let formattedDate = `${date.toLocaleDateString()} at ${date.toLocaleTimeString()}`;
    return formattedDate;
}

interface VideosProps extends WithRouterProps {
    router: NextRouter;
}

interface VideosState {
    youtubeId: string,
    videoPreview: { title: string, thumbnailUrl: string }
    selectedLanguage: Language,
    isGenerating: boolean,
    videos: Array<any>
}

class Videos extends Component<VideosProps, VideosState> {
    static async getInitialProps({ query }: any) {
        const { youtubeId } = query;
        return { youtubeId };
    }

    constructor(props: any) {
        super(props);
        this.state = {
            youtubeId: "", // "XcvhERcZpWw" // tv-_1er1mWI
            videoPreview: { title: "", thumbnailUrl: "" },
            selectedLanguage: { name: "English", code: "eng_Latn" },
            isGenerating: false,
            videos: []
        };

        this.handleGenerateClick = this.handleGenerateClick.bind(this);
        this.setSelectedLanguage = this.setSelectedLanguage.bind(this);
    }

    componentDidMount() {
        const { router } = this.props;
        const youtubeId = router.query["youtubeId"] as string;
        this.setState({
            ...this.state,
            youtubeId
        });
        if (youtubeId) {
            this.getVideoPreview(youtubeId)
        }
        this.fetchAllVideoSummaries();
    }

    async fetchAllVideoSummaries() {
        const { router } = this.props;
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
            this.setState({
                videos: data
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
            if (response.status == 403) {
                router.push('/login');
            }
        }
    }

    async getVideoPreview(youtubeId: string) {
        const { router } = this.props;
        const token = localStorage.getItem('concise_access_token');

        const response = await fetch(`http://localhost:8080/videos/preview/` + youtubeId, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const data = await response.json();
            this.setState({
                videoPreview: data
            })
        } else {
            console.log(`Error fetching data: ${response.status} ${response.statusText}`);
            if (response.status == 403) {
                router.push('/login');
            }
        }
    }

    // TODO: Refactor to expect async responses per chapter. Ensure support for user closing/reopening application.
    async handleGenerateClick() {
        this.setState({
            isGenerating: true
        })
        const token = localStorage.getItem('concise_access_token');
        const response = await fetch(`http://localhost:8080/videos/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                summaryLanguage: this.state.selectedLanguage.code,
                youtubeId: this.state.youtubeId
            })
        });
        if (response.ok) {
            const data = await response.json();
            this.setState({
                isGenerating: false
            })
            const { router } = this.props;
            router.push('/video' + '?videoId=' + data["id"]);
        } else {
            this.setState({
                isGenerating: false
            })
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
                {this.state.youtubeId &&(
                    <Heading as="h2">Generate</Heading>
                )}
                {this.state.youtubeId && (
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
                                <Select value={this.state.selectedLanguage.code} onChange={this.setSelectedLanguage}
                                        disabled={this.state.isGenerating}>
                                    {languages.map((language: Language) => (
                                        <Select.Option key={language.code} value={language.code}>{language.name}</Select.Option>
                                    ))}
                                </Select>
                                <Button variant="primary" onClick={this.handleGenerateClick} disabled={this.state.isGenerating}>Generate Summary</Button>
                            </div>
                        </div>
                    </div>
                )}
                <Heading as="h2">History</Heading>
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

export default withRouter(Videos);