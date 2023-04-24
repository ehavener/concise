import styles from '../styles/signUp.module.css';

export default function SignUp() {



    return (
        <div className={styles.container}>
            <div className={styles.content}>
                <p style={{textAlign: "center"}}>Sign Up</p>
                <input type="email" name="email" placeholder="Email"/>
                <input type="password" name="password" placeholder="Password"/>
                <button type="button">Submit</button>
            </div>
        </div>
    )
}
