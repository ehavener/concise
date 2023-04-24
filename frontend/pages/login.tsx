import styles from "@/styles/login.module.css";
import {Component} from "react";

export default class Login extends Component<any, any> {
    constructor(props: any) {
        super(props);
        this.state = {
            email: '',
            password: ''
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
        const url = "http://127.0.0.1:8080/api/v1/users/login"
        fetch(url, {
            method: "POST",
            mode: "cors",
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                email: this.state.email,
                password: this.state.password
            })
        })
            .then((response) => response.json())
            .then(data => {
                this.setState({ ...this.state, bearer: data['access_token'] })
                localStorage.setItem("ot_access_token", data['access_token']);
                // window.location.replace(`${window.location.origin}/`);
            })

        event.preventDefault();
    }

    render() {
        return (
            <div className={styles.container}>
                <div className={styles.content}>
                    <p style={{textAlign: "center"}}>Login</p>
                    <input type="email" name="email" placeholder="Email" value={this.state.email} onChange={this.handleEmailChange}/>
                    <input type="password" name="password" placeholder="Password" value={this.state.password} onChange={this.handlePasswordChange}/>
                    <button type="button" onClick={this.handleSubmit}>Submit</button>
                </div>
            </div>
        )
    }
}