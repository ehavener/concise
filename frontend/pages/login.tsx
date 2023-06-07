import styles from "@/styles/login.module.css";
import {Component} from "react";
import {WithRouterProps} from "next/dist/client/with-router";
import {NextRouter, withRouter} from "next/router";
import {Language} from "@/pages/languages";
import {TextInput} from '@primer/react-brand'
import {Button} from '@primer/react-brand'
import {Heading} from '@primer/react-brand'

interface LoginProps extends WithRouterProps {
    router: NextRouter;
}

interface LoginState {
    email: string,
    password: string,
    bearer: string
}

class Login extends Component<LoginProps, LoginState> {
    constructor(props: any) {
        super(props);
        this.state = {
            email: '',
            password: '',
            bearer: ''
        };

        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleEmailChange = this.handleEmailChange.bind(this);
        this.handlePasswordChange = this.handlePasswordChange.bind(this);
    }

    handleEmailChange(event: any) {
        this.setState({
            email: event.target.value
        });
    }

    handlePasswordChange(event: any) {
        this.setState({
            password: event.target.value
        });
    }

    handleSubmit(event: any) {
        const { router } = this.props;
        const url = "http://127.0.0.1:8080/authenticate"
        fetch(url, {
            method: "POST",
            mode: "cors",
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                login: this.state.email,
                password: this.state.password
            })
        })
            .then((response) => response.json())
            .then(data => {
                this.setState({ ...this.state, bearer: data['accessToken'] })
                localStorage.setItem("concise_access_token", data['accessToken']);
                router.push('/videos');
            })

        event.preventDefault();
    }

    // TODO: styles and layout for login page
    render() {
        return (
            <div className={styles.container}>
                <div className={styles.content}>
                    <Heading as="h3">Login</Heading>
                    <br/>
                    <TextInput type="email" name="email" placeholder="Email" value={this.state.email} onChange={this.handleEmailChange}/>
                    <br/>
                    <TextInput type="password" name="password" placeholder="Password" value={this.state.password} onChange={this.handlePasswordChange}/>
                    <br/>
                    <Button type="button" onClick={this.handleSubmit}>Submit</Button>
                </div>
            </div>
        )
    }
}

export default withRouter(Login);
