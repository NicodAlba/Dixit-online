import {Card,CardBody,CardTitle,CardText,Col} from "reactstrap";
export default function StatCard({title,value,unit,colSize="4"}) {
    return <Col md={colSize} sm="6" className="mb-3"><Card className="stats-card"><CardBody className="stats-card-body"><CardTitle tag="h3" className="stats-card-title">{title}</CardTitle><CardText className="stats-card-value">{value}{unit&&<span className="stats-unit">{unit}</span>}</CardText></CardBody></Card></Col>;
}
